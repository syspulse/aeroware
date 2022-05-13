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
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfile

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
 
  val wallet = new WalletVaultKeyfile(config.keystore, config.keystorePass)
  
  val wr = wallet.load()
  log.info(s"wallet: ${wr}")
  val signerAddr = wallet.signers.toList.head._2.head.addr

  val validationEngine = new ValidationEngineADSB()
  val rewardEngine = new RewardEngineADSB()
  val fleet = new Fleet(config)

  val validator = Flow[MSG_MinerData].map( m => { 
    val v1 = validationEngine.validate(m)
    if(v1) {
      val miner = fleet.+(m.pk)
      val reward = rewardEngine.calculate(m)
      miner.rewards.+(reward)
    }

    log.info(s"\n${fleet.toString()}")

    m
  })

  val retrySettings = RestartSettings(
    minBackoff = 1.seconds,
    maxBackoff = 3.seconds,
    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
  )
  val mqttClient = new MQTTClientSubscriber(MQTTConfig(host=config.mqttHost,port=config.mqttPort,clientId=s"adsb-validator-${signerAddr}"))
  val mqttSource =  { 
    RestartSource.onFailuresWithBackoff(retrySettings) { 
      log.info(s"-> MQTT(${config.mqttHost}:${config.mqttPort})")
      () => mqttClient.source() 
    }
  }

  def run() = {
    val validatorFlow = mqttSource
      .via(mqttClient.mqttSubscriber)
      .via(validator)
      .runWith(Sink.ignore)

    log.info(s"validatorFlow: ${validatorFlow}")  
  }
      
}
