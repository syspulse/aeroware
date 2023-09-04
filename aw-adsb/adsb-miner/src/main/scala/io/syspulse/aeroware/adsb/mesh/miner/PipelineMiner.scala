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
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfile
import io.syspulse.skel.crypto.wallet.WalletVault

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
import akka.stream.RestartSettings

import java.util.concurrent.atomic.AtomicLong
import jakarta.validation.constraints.Min

class MinerStat {
  val total = new AtomicLong()
  def +(sz:Long):Long = {
    total.addAndGet(sz)
  }
  override def toString = s"${total}"
}

class PipelineMiner(feed:String,output:String)(implicit config:Config)
  extends Pipeline[ADSB,MSG_MinerData,MSG_MinerData](feed,output,config.throttle,config.delimiter,config.buffer) {

  implicit protected val log = Logger(s"${this}")
  //implicit val ec = system.dispatchers.lookup("default-executor") //ExecutionContext.global
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val minerStat = new MinerStat()

  val wallet = new WalletVaultKeyfile(config.keystore, config.keystorePass)  
  val wr = wallet.load()
  
  val signerPk = wallet.signers.values.head.pk
  val signerAddr = wallet.signers.values.head.addr
  val signerAddrBytes = Util.fromHexString(signerAddr)
  
  val defaultRetrySetting = RestartSettings(
    minBackoff = FiniteDuration(3000,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(10000,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  )
  .withMaxRestarts(10, FiniteDuration(5,TimeUnit.MINUTES))

  val connectTimeout = 1000L
  val idleTimeout = 1000L
  
  val dataEncoder = Flow[MSG_MinerData].map( md => {      
      val mqttData = upickle.default.writeBinary(md)
      log.debug(s"encoded = ${Util.hex(mqttData)}")

      val wireData = if(MSG_Options.isV1(config.protocolOptions)) Util.hex(mqttData).getBytes else mqttData

      // healthcheck for decoding
      //val decodedMsg = upickle.default.readBinary[MSG_MinerData](wireData)
      //log.info(s"decodedMsg: ${decodedMsg}")      
      wireData
    })

  // MQTT
  def toMQTT(mqttHost:String,mqttPort:Int,mqttTopic:String="adsb",clientId:String="adsb-client") = {
    
    val mqttClientId = s"${clientId}"
    //val mqttConnectionId = s"${math.abs(Random.nextLong())}"

    val mqttConnectionSettings = MqttConnectionSettings(
      broker = s"tcp://${mqttHost}:${mqttPort}", 
      clientId = mqttClientId,
      persistence = new MemoryPersistence
    ).withAutomaticReconnect(true)
    
    
    val mqttSink: Sink[MqttMessage, Future[Done]] = MqttSink(mqttConnectionSettings, MqttQoS.AtLeastOnce)
          
    val mqttPublisher = Flow[Array[Byte]].map( data => {
      log.debug(s"Payload[${Util.hex(data)}] -> MQTT(${mqttHost}:${mqttPort})")
      MqttMessage(mqttTopic,ByteString(data))  
    })

    val sink = dataEncoder
      .via(mqttPublisher)      
      .toMat(mqttSink)(Keep.both)
             
    RestartSink.withBackoff(defaultRetrySetting) { 
      log.info(s"-> MQTT(${mqttHost}:${mqttPort})")
      () => sink
    }  
  }
    
  def fromTcp(host:String,port:Int) = {
    val ip = InetSocketAddress.createUnresolved(host, port)
    val conn = Tcp().outgoingConnection(
      remoteAddress = ip,
      connectTimeout = Duration(connectTimeout,TimeUnit.MILLISECONDS),
      idleTimeout = Duration(idleTimeout,TimeUnit.MILLISECONDS)
    )
    val sourceRestarable = RestartSource.withBackoff(defaultRetrySetting) { () => 
      log.info(s"Connecting -> dump1090(${host}:${port})...")
      Source.actorRef(1, OverflowStrategy.fail)
        .via(conn)
        .log("dump1090")
    }
    sourceRestarable
  }
    
  override def source() = {
    feed.split("://").toList match {
      case "dump1090" :: _ | "tcp" :: _ => {
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
        toMQTT(uri.host,uri.port.toInt, clientId = signerAddr)
      }
      
      case "raw" :: _ => {
        val sink = 
          dataEncoder
          .toMat(Sink.foreach{d => {
                        
            if(MSG_Options.isV1(config.protocolOptions)) {
              // Textual protocol
              println(Util.hex(d))              
            } else {
              // binary protocol
              d.foreach(b => print(b.toChar))
            }
              
          }})(Keep.both)
        sink
      }

      case "hex" :: _ => {
        val sink = 
          dataEncoder
          .toMat(Sink.foreach{d => {
            println(Util.hex(d))            
          }})(Keep.both)
        sink
      }

      case _ => super.sink()
    }
  }

  val encoder = Flow[Seq[ADSB]].map( aa => { 
    val data = aa.map(a => MSG_MinerADSB(a.ts,a.raw)).toArray
    
    val msg = MSG_MinerData(
      ts = System.currentTimeMillis(),
      addr = signerAddrBytes,
      data = data,
      sig = MinerSig.empty,
      ops = config.protocolOptions
    )

    msg
  })

  val signer = Flow[MSG_MinerData].map( msg => { 
    val data = upickle.default.writeBinary(msg.data)
    val sig = wallet.msign(data,None, None).head

    msg.copy(
      sig = MinerSig(sig)
    )    
  })

  val checksum = Flow[MSG_MinerData].map( msg => { 
    val data = msg.data
    val sigData = upickle.default.writeBinary(data)
    val sig = Util.hex(msg.sig.r) + ":" + Util.hex(msg.sig.s)

    val v = wallet.mverifyAddress(List(sig),sigData,Seq(signerAddr))
    if(v == 0) {
      log.error(s"Signature: INVALID: ${msg.sig}")
    }else
      log.debug(s"Signature: OK: ${msg.sig}")
    
    msg
  })

  val stat = Flow[MSG_MinerData].map( msg => { 
    minerStat.+(msg.data.size)
    log.info(s"stat=[${msg.data.size},${minerStat}]")
    msg
  })

  override def process:Flow[ADSB,MSG_MinerData,_] = 
    Flow[ADSB]
      .groupedWithin(config.blockSize, FiniteDuration(config.blockWindow,TimeUnit.MILLISECONDS))
      .via(encoder)
      .via(signer)
      .via(checksum)
      .via(stat)
      //.via(dataEncoder)
  
  
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
