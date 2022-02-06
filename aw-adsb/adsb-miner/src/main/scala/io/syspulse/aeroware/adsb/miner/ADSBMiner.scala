package io.syspulse.aeroware.adsb.miner

import java.nio.file.{Path,Paths, Files}

import scala.util.{Try,Failure,Success}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl.LogRotatorSink
import akka.util.ByteString
import akka.NotUsed
import akka.actor.ActorSystem

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format._

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.ingest.AdsbIngest
import akka.stream.alpakka.mqtt.streaming.MqttSessionSettings
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttClientSession
import akka.stream.alpakka.mqtt.streaming.scaladsl.Mqtt
import akka.stream.alpakka.mqtt.streaming.MqttCodec
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.alpakka.mqtt.streaming.Command
import scala.concurrent.Future
import akka.stream.alpakka.mqtt.streaming.Connect
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import scala.util.Random

case class ADSB_Mined_SignedData(
  ts:Long,
  raw:Raw
)

object ADSB_Mined_SignedData {
  implicit val rw: RW[ADSB_Mined_SignedData] = macroRW
}

class ADSBMiner(config:Config) extends AdsbIngest {
  
  import MSG_MinerData._
  import MSG_MinerADSB._
 
  val wallet = new WalletVaultKeyfiles(config.keystoreDir, (keystoreFile) => {config.keystorePass})
  
  val wr = wallet.load()
  log.info(s"wallet: ${wr}")
  val signerAddr = wallet.signers.toList.head._2.head.addr

  val sinkRestartable =  { 
    RestartSink.withBackoff(retrySettings) { () =>
      Sink.foreach[MSG_Miner](m => println(s"${m}"))
    }
  }

  // ==============================================================================================
  val mqttHost = "localhost"
  val mqttPort = 1883
  val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
  val mqttSession = ActorMqttClientSession(mqttSettings)
  val mqttConnection = Tcp().outgoingConnection(mqttHost, mqttPort)
  val mqttClientId = "ADSB-MQTT-Client-1"
  val mqttConnectionId = "1"
  val mqttTopc = "adsb-topic"

  val mqttFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    Mqtt
      .clientSessionFlow(mqttSession, ByteString(mqttConnectionId))
      .join(mqttConnection)

  val (mqttQueue: SourceQueueWithComplete[Command[Nothing]], events: Future[Publish]) =
    Source
      .queue(10, OverflowStrategy.fail)
      .via(mqttFlow)
      .collect {
        case Right(Event(p: Publish, _)) => {
          log.debug(s"${p}")
          p
        }
      }
      .log(s"MQTT(${mqttHost}:${mqttPort}): ")
      .async
      .toMat(Sink.head)(Keep.both)
      .run()

  mqttQueue.offer(Command(Connect(mqttClientId, ConnectFlags.CleanSession)))
  // mqttQueue.offer(Command(Subscribe(mqttTopc)))
  
  val mqtt = Flow[MSG_MinerData].map( md => {
    val mqttData = Util.hex2(md.toString.getBytes())
    log.info(s"=> MQTT(${mqttHost}:${mqttPort}): ${mqttConnection}: ${mqttData}")
    mqttSession ! Command(
      //Publish(ControlPacketFlags.RETAIN | ControlPacketFlags.QoSAtLeastOnceDelivery, mqttTopc, mqttData)
      Publish(ControlPacketFlags.QoSAtLeastOnceDelivery, mqttTopc, ByteString(mqttData))
    )
    md
  })
  // ===============================================================================================

  val signer = Flow[Seq[ADSB]].map( aa => { 
    val adsbData = aa.map(a => MSG_MinerADSB(a.ts,a.raw)).toArray
    val sigData = upickle.default.writeBinary(adsbData)
    val sig = wallet.msign(sigData,None, None).head

    val msgData = MSG_MinerData(
      ts = System.currentTimeMillis(),
      addr = Util.fromHexString(signerAddr),
      adsbs = adsbData,
      sig = MinerSig(sig)
    )

    msgData
  })

  val verifier = Flow[MSG_MinerData].map( m => { 
    val adsbData = m.adsbs
    val sigData = upickle.default.writeBinary(adsbData)
    val sig = Util.hex2(m.sig.r) + ":" + Util.hex2(m.sig.s)

    val v = wallet.mverify(List(sig),sigData,None,None)
    if(v == 0) {
      log.error(s"NOT VERIFIED: ${m.sig}")
    }else
      log.info(s"Verified: ${m.sig}")
    m
  })

  def run() = {
    val adsbSource = flow(config.ingest)
    
    val adsbFlow = adsbSource
      .groupedWithin(config.batchSize, FiniteDuration(config.batchWindow,TimeUnit.MILLISECONDS))
      .via(signer)
      .via(mqtt)
      .via(verifier)
      //.map(a => ByteString(a.toString))
      .log(s"output -> ")
      .toMat(sinkRestartable)(Keep.both)
      .run()

    adsbFlow
  }
    
}
