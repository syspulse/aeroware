package io.syspulse.aeroware.adsb.mesh.validator

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
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.ingest.AdsbIngest
import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.transport.{ MQTTClientSubscriber, MQTTConfig}
import io.syspulse.aeroware.adsb.mesh.transport.MQTTServerFlow
import scala.concurrent.ExecutionContext
import io.syspulse.aeroware.adsb.mesh.transport.MQTTClientSubscriber

class Validator(config:Config) {
  implicit val log = Logger(s"${this.getClass().getSimpleName()}")
  implicit val system = ActorSystem("ActorSystem-Validator")
  implicit val ec = ExecutionContext.global

  import MSG_MinerData._
  import MSG_MinerADSB._
 
  val wallet = new WalletVaultKeyfiles(config.keystoreDir, (keystoreFile) => {config.keystorePass})
  
  val wr = wallet.load()
  log.info(s"wallet: ${wr}")
  val signerAddr = wallet.signers.toList.head._2.head.addr

  val validationEngine = new ValidationEngineADSB()
  val rewardEngine = new RewardEngineADSB()
  val fleet = new Fleet(config)

  val validator = Flow[MSG_MinerData].map( m => { 
    // val adsbData = m.adsbs
    // val sigData = upickle.default.writeBinary(adsbData)
    // val sig = Util.hex2(m.sig.r) + ":" + Util.hex2(m.sig.s)

    // val pk = m.pk
    // val v = Eth.verify(sigData,sig,pk) //wallet.mverify(List(sig),sigData,None,None)
    // if(!v) {
    //   log.error(s"Invalid signature: ${m.sig}")
    // }else
    //   log.info(s"Verified: ${m.sig}")
    val v1 = validationEngine.validate(m)
    if(v1) {
      val miner = fleet.+(m.pk)
      val reward = rewardEngine.calculate(m)
      miner.rewards.+(reward)
    }

    log.info(s"\n${fleet.toString()}")

    m
  })

  def run() = {
    val mqtt = new MQTTClientSubscriber(MQTTConfig(host=config.mqttHost,port=config.mqttPort,clientId=s"adsb-validator-${signerAddr}")).run(
      validator
    )
  }
      
}
