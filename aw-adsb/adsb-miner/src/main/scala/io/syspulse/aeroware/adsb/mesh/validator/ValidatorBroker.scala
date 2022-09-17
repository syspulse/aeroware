package io.syspulse.aeroware.adsb.mesh.validator

import java.nio.file.{Path,Paths, Files}

import scala.util.{Try,Failure,Success}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.NotUsed
import akka.actor.ActorSystem

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Random

import com.typesafe.scalalogging.Logger

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format._

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfile

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw

import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.transport.{ MQTTConfig}
import io.syspulse.aeroware.adsb.mesh.transport.MQTTServerFlow

class ValidatorBroker(config:Config) {
  implicit val log = Logger(s"${this.getClass().getSimpleName()}")
  implicit val system = ActorSystem("ActorSystem-ValidatorBroker")
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
    val r1 = validationEngine.validate(m)
    
    val miner = fleet.+(m.pk)
    if(r1 >= 0.0) {
      val reward = rewardEngine.calculate(m) + r1
      miner.rewards.+(reward)
    } else {
      // penalty 
      miner.rewards.+(r1)
    }

    log.info(s"\n${fleet.toString()}")

    m
  })

  val retrySettings = RestartSettings(
    minBackoff = 1.seconds,
    maxBackoff = 3.seconds,
    randomFactor = 0.2 
  )
  
  val mqttServer = new MQTTServerFlow(
    MQTTConfig(host=config.mqttHost,port=config.mqttPort,clientId=s"adsb-validator-${signerAddr}")
  )
  
  //val mqttSource =  mqttServer.source()
  // { 
  //   RestartSource.onFailuresWithBackoff(retrySettings) { 
  //     log.info(s"-> MQTT(${config.mqttHost}:${config.mqttPort})")
  //     () => mqttClient.source() 
  //   }
  // }

  def run() = {
    val validatorFlow = mqttServer.flow()
      .map(e => {
        println(s"${e}")
        e
      })
      .via(validator)
      .runWith(Sink.ignore)

    log.info(s"validatorFlow: ${validatorFlow}")  
  }
      
}
