package io.syspulse.aeroware.adsb.mesh.miner

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

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfile

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.ingest.old.AdsbIngest
import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.transport.{ MQTTClientPublisher, MQTTConfig}
import scala.concurrent.ExecutionContext
import io.syspulse.aeroware.adsb.mesh.transport.MQTTClientPublisher

case class ADSB_Mined_SignedData(
  ts:Long,
  raw:Raw
)

object ADSB_Mined_SignedData {
  implicit val rw: RW[ADSB_Mined_SignedData] = macroRW
}

class Miner(config:Config) extends AdsbIngest {
  // implicit val ec = ExecutionContext.global

  // import MSG_MinerData._
  // import MSG_MinerADSB._
 
  // val wallet = new WalletVaultKeyfile(config.keystore, config.keystorePass)
  
  // val wr = wallet.load()
  // log.info(s"wallet: ${wr}")
  // val signerPk = wallet.signers.toList.head._2.head.pk
  // val signerAddr = wallet.signers.toList.head._2.head.addr

  // val mqttClient = new MQTTClientPublisher(MQTTConfig(host=config.mqttHost,port=config.mqttPort,clientId=s"adsb-miner-${signerAddr}"))
  // val mqttSink =  { 
  //   RestartSink.withBackoff(retrySettings) { 
  //     log.info(s"-> MQTT(${config.mqttHost}:${config.mqttPort})")
  //     () => mqttClient.sink() 
  //   }
  // }

  // val signer = Flow[Seq[ADSB]].map( aa => { 
  //   val adsbData = aa.map(a => MSG_MinerADSB(a.ts,a.raw)).toArray
  //   val sigData = upickle.default.writeBinary(adsbData)
  //   val sig = wallet.msign(sigData,None, None).head

  //   val msgData = MSG_MinerData(
  //     ts = System.currentTimeMillis(),
  //     pk = signerPk,
  //     adsbs = adsbData,
  //     sig = MinerSig(sig)
  //   )

  //   msgData
  // })

  // val checksum = Flow[MSG_MinerData].map( m => { 
  //   val adsbData = m.adsbs
  //   val sigData = upickle.default.writeBinary(adsbData)
  //   val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)

  //   val v = wallet.mverify(List(sig),sigData,None,None)
  //   if(v == 0) {
  //     log.error(s"Invalid signature: ${m.sig}")
  //   }else
  //     log.info(s"Verified: ${m.sig}")
  //   m
  // })

  // def run() = {
  //   val adsbSource = flow(config.ingest)
    
  //   val minerFlow = adsbSource
  //     .groupedWithin(config.batchSize, FiniteDuration(config.batchWindow,TimeUnit.MILLISECONDS))
  //     .via(signer)
  //     .via(checksum)
  //     .via(mqttClient.flow())
  //     .toMat(mqttSink)(Keep.both)
  //     .run()

  //   minerFlow
  // }
    
}
