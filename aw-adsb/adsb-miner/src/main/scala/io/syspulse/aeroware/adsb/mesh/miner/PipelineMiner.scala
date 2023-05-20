package io.syspulse.aeroware.adsb.mesh.miner

import scala.util.Random
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.{ Success,Failure}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.javadsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

import akka.stream.scaladsl.Tcp

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.serde.Parq._

import java.util.concurrent.TimeUnit
import java.net.InetSocketAddress
import akka.stream.scaladsl.RestartSource
import akka.stream.OverflowStrategy

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.ingest.Dump1090URI
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingested

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
//import io.syspulse.aeroware.adsb.mesh.transport.MQTTClientPublisher
//import io.syspulse.aeroware.adsb.mesh.transport.MQTTConfig
import io.syspulse.aeroware.adsb.mesh.transport.MqttURI

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import scala.concurrent.Future
import scala.util.Random

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import io.syspulse.aeroware.adsb.mesh.miner

import scala.concurrent.ExecutionContext
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Options
import io.swagger.v3.oas.models.security.SecurityScheme.In
import java.net.InetSocketAddress
import akka.stream.alpakka.mqtt.MqttConnectionSettings
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import akka.stream.alpakka.mqtt.MqttMessage
import akka.Done
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.MqttQoS
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RestartSink
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerADSB
import io.syspulse.aeroware.adsb.mesh.protocol.MinerSig

class PipelineMiner(feed:String,output:String)(implicit config:Config)
  extends Pipeline[ADSB,MSG_MinerData,MSG_MinerData](feed,output,config.throttle,config.delimiter,config.buffer) {

  implicit protected val log = Logger(s"${this}")
  //implicit val ec = system.dispatchers.lookup("default-executor") //ExecutionContext.global
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val wallet = new skel.crypto.wallet.WalletVaultKeyfile(config.keystore, config.keystorePass)  
  val wr = wallet.load()
  log.info(s"wallet: ${wr}")
  val signerPk = wallet.signers.toList.head._2.head.pk
  val signerAddr = wallet.signers.toList.head._2.head.addr
  

  val connectTimeout = 1000L
  val idleTimeout = 1000L
  
  // MQTT
  def toMQTT(mqttHost:String,mqttPort:Int,mqttTopic:String="adsb",clientId:String="adsb-client",protocolVer:Int = 0) = {
    val mqttConnectionSettings = MqttConnectionSettings(
      broker = s"tcp://${mqttHost}:${mqttPort}", 
      clientId = clientId, 
      persistence = new MemoryPersistence
    ).withAutomaticReconnect(true)

    val mqttClientId = clientId
    val mqttConnectionId = s"${math.abs(Random.nextLong())}"
    
    val mqttSink: Sink[MqttMessage, Future[Done]] = MqttSink(mqttConnectionSettings, MqttQoS.AtLeastOnce)
      
    val mqttPublisher = Flow[MSG_MinerData].map( md => {
      
      val mqttData = upickle.default.writeBinary(md)
      log.debug(s"encoded = ${Util.hex(mqttData)}")

      val wireData = if(MSG_Options.isV1(protocolVer)) Util.hex(mqttData).getBytes else mqttData

      log.debug(s"Payload[${Util.hex(wireData)}] -> MQTT(${mqttHost}:${mqttPort})")
      MqttMessage(mqttTopic,ByteString(wireData))  
    })

    val sink = mqttPublisher.toMat(mqttSink)(Keep.both)
             
    RestartSink.withBackoff(retrySettings) { 
      log.info(s"-> MQTT(${mqttHost}:${mqttPort})")
      () => sink
    }
  
  }
  
  def filter:Seq[String] = config.filter

  def fromTcp(host:String,port:Int) = {
    val ip = InetSocketAddress.createUnresolved(host, port)
    val conn = Tcp().outgoingConnection(
      remoteAddress = ip,
      connectTimeout = Duration(connectTimeout,TimeUnit.MILLISECONDS),
      idleTimeout = Duration(idleTimeout,TimeUnit.MILLISECONDS)
    )
    val sourceRestarable = RestartSource.withBackoff(retrySettings) { () => 
      log.info(s"Connecting -> dump1090(${host}:${port})...")
      Source.actorRef(1, OverflowStrategy.fail)
        .via(conn)
        .log("dump1090")
    }
    sourceRestarable
  }
    
  override def source() = {
    feed.split("://").toList match {
      case "dump1090" :: _ => {
        val uri = Dump1090URI(feed)
        fromTcp(uri.host,uri.port.toInt)
      }
      case _ => super.source()
    }
  }

  override def sink() = {
    output.split("://").toList match {
      case "mqtt" :: _ => {
        val uri = MqttURI(output)
        toMQTT(uri.host,uri.port.toInt)
      }
      case _ => super.sink()
    }
  }

  val signer = Flow[Seq[ADSB]].map( aa => { 
    val adsbData = aa.map(a => MSG_MinerADSB(a.ts,a.raw)).toArray
    val sigData = upickle.default.writeBinary(adsbData)
    val sig = wallet.msign(sigData,None, None).head

    val msgData = MSG_MinerData(
      ts = System.currentTimeMillis(),
      pk = signerPk,
      adsbs = adsbData,
      sig = MinerSig(sig)
    )

    msgData
  })

  val checksum = Flow[MSG_MinerData].map( m => { 
    val adsbData = m.adsbs
    val sigData = upickle.default.writeBinary(adsbData)
    val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)

    val v = wallet.mverify(List(sig),sigData,None,None)
    if(v == 0) {
      log.error(s"Invalid signature: ${m.sig}")
    }else
      log.info(s"Verified: ${m.sig}")
    m
  })


  override def process:Flow[ADSB,MSG_MinerData,_] = 
    Flow[ADSB]
      .groupedWithin(config.batchSize, FiniteDuration(config.batchWindow,TimeUnit.MILLISECONDS))
      .via(signer)
      .via(checksum)
  
  
  def decode(data:String,ts:Long):Option[ADSB] = {
    Decoder.decode(data,ts) match {
      case Success(a) => Some(a)
      case Failure(e) => None
    }
  }

  def parse(data:String):Seq[ADSB] = {
    if(data.isEmpty()) return Seq()
    try {
      val a = data.trim.split("\\s+").toList match {
        case ts :: a :: Nil => decode(a,ts.toLong)
        case a :: Nil => decode(a,System.currentTimeMillis())
        case _ => {
          log.error(s"failed to parse: invalid format: ${data}")
          return Seq.empty
          None
        }
      }
      
      log.debug(s"adsb=${a}")
      a.toSeq

    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }

  def transform(a: MSG_MinerData): Seq[MSG_MinerData] = Seq(a)
  
}
