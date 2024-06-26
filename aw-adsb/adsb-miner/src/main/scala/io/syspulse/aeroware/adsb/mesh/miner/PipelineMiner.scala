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
import io.syspulse.skel.ingest.flow.Flows

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
//import io.syspulse.aeroware.adsb.mesh.transport.MQTTClientPublisher
//import io.syspulse.aeroware.adsb.mesh.transport.MQTTConfig
import io.syspulse.aeroware.adsb.mesh.transport.MqttURI

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import scala.concurrent.Future
import scala.util.Random

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
import akka.stream.RestartSettings

import java.util.concurrent.atomic.AtomicLong
import jakarta.validation.constraints.Min

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfile
import io.syspulse.skel.crypto.wallet.WalletVault

import io.syspulse.aeroware.core.Minable

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import io.syspulse.aeroware.adsb.mesh.miner

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerPayload
import io.syspulse.aeroware.adsb.mesh.protocol.MinerSig

import io.syspulse.aeroware.adsb.mesh.PayloadTypes
import io.syspulse.aeroware.adsb.mesh.payload.PayloadType

class MinerStat {
  val total = new AtomicLong()
  def +(sz:Long):Long = {
    total.addAndGet(sz)
  }
  override def toString = s"${total}"
}


abstract class PipelineMiner(feed:String,output:String)(implicit config:Config)
  extends Pipeline[Minable,MSG_MinerData,MSG_MinerData](feed,output,config.throttle,config.delimiter,config.buffer) {

  implicit protected val log = Logger(s"${this}")
  //implicit val ec = system.dispatchers.lookup("default-executor") //ExecutionContext.global
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def getPayloadType():PayloadType = ???

  val minerStat = new MinerStat()

  val wallet = new WalletVaultKeyfile(config.keystore, config.keystorePass)  
  val wr = wallet.load()
  
  val signerPk = wallet.signers.values.head.pk
  val signerAddr = wallet.signers.values.head.addr
  val signerAddrBytes = Util.fromHexString(signerAddr)
  
  val retry = RestartSettings(
    minBackoff = FiniteDuration(config.timeoutRetry,TimeUnit.MILLISECONDS),
    maxBackoff = FiniteDuration(config.timeoutRetry,TimeUnit.MILLISECONDS),
    randomFactor = 0.2
  )
  //.withMaxRestarts(10, FiniteDuration(5,TimeUnit.MINUTES))

  // val connectTimeout = 1000L
  // val idleTimeout = 1000L
  
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
  def toMQTT(mqttHost:String,mqttPort:Int,
             //mqttTopic:String="adsb",
             mqttTopic:String=config.entity,
             clientId:String="aw-miner") = {
    
    val mqttClientId = s"${clientId}"
    
    // Don't enable "withAutomaticReconnect(true)", RestartSink will reconnect with a log message !
    val mqttConnectionSettings = MqttConnectionSettings(
      broker = s"tcp://${mqttHost}:${mqttPort}", 
      clientId = mqttClientId,
      persistence = new MemoryPersistence
    )
    //.withAutomaticReconnect(true)
    .withConnectionTimeout(FiniteDuration(config.timeoutConnect,TimeUnit.MILLISECONDS))
    .withDisconnectTimeout(FiniteDuration(config.timeoutIdle,TimeUnit.MILLISECONDS))
    
    
    val mqttSink: Sink[MqttMessage, Future[Done]] = MqttSink(mqttConnectionSettings, MqttQoS.AtLeastOnce)
          
    val mqttPublisher = Flow[Array[Byte]].map( data => {
      log.debug(s"Payload[${Util.hex(data)}] -> mqtt://${mqttHost}:${mqttPort}/${mqttTopic}")
      MqttMessage(mqttTopic,ByteString(data))  
    })

    val sink = dataEncoder
      .via(mqttPublisher)      
      .toMat(mqttSink)(Keep.both)
             
    RestartSink.withBackoff(retry) { () => 
      log.info(s"Connecting -> mqtt://${mqttHost}:${mqttPort}/${mqttTopic}")
      sink
    }  
  }
      
  override def source() = {
    feed.split("://").toList match {
      // case "dump1090" :: _ => 
      //   val uri = Dump1090URI(feed)
      //   Flows.fromTcpClient(uri.host,uri.port.toInt, 
      //     connectTimeout = config.timeoutConnect, idleTimeout = config.timeoutIdle,
      //     retry = retry
      //   )
      case "tcp" :: uri :: Nil => 
        Flows.fromTcpClientUri(uri, 
          connectTimeout = config.timeoutConnect, idleTimeout = config.timeoutIdle,
          retry = retry
        )
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

  val encoder = Flow[Seq[Minable]].map( aa => { 

    // timestamp of the Event/Message (e.g. ADSB received from hardware, like dump1090)
    val data = aa.map(a => MSG_MinerPayload(
      a.ts,
      getPayloadType(), 
      a.raw)
    ).toArray
    
    // timestamp of the Miner data with possible jitter
    val tsData = System.currentTimeMillis + config.jitter

    val msg = MSG_MinerData(
      ts = tsData,
      addr = signerAddrBytes,
      payload = data,
      sig = MinerSig.empty,
      ops = config.protocolOptions
    )

    msg
  })

  val signer = Flow[MSG_MinerData].map( msg => { 
    val data = upickle.default.writeBinary(msg.payload)
    val sig = wallet.msign(data,None, None).head

    msg.copy(
      sig = MinerSig(sig)
    )    
  })

  val checksum = Flow[MSG_MinerData].map( msg => { 
    val data = msg.payload
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
    minerStat.+(msg.payload.size)
    log.info(s"stat=[${msg.payload.size},${minerStat}]")
    msg
  })

  override def process:Flow[Minable,MSG_MinerData,_] = 
    Flow[Minable]
      .groupedWithin(config.blockSize, FiniteDuration(config.blockWindow,TimeUnit.MILLISECONDS))
      .via(encoder)
      .via(signer)
      .via(checksum)
      .via(stat)
      //.via(dataEncoder)
    
  // decoding is for validation only
  // to be compatible with Validator to avoid penalty
  // If protocols are fully compatible, this step can be
  // totally omitted since validator expect raw data
  def decode(data:String,ts:Long):Option[Minable] = ???

  def preparse(data:String):List[String] = ???

  def parse(data:String):Seq[Minable] = {
    // skip empty lines 
    if(data.trim.isEmpty()) return Seq()
    try {      
      val a = preparse(data) match {
        case ts :: a :: Nil => decode(a,ts.toLong)
        case a :: Nil => decode(a,System.currentTimeMillis())
        case _ => {
          log.error(s"failed to pre-parse: invalid format: ${data}")
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
