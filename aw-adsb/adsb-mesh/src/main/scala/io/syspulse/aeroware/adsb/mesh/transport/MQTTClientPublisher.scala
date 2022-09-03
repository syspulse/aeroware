package io.syspulse.aeroware.adsb.mesh.transport

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

import scala.concurrent.Future
import scala.util.Random

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
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

class MQTTClientPublisher(config:MQTTConfig)(implicit val as:ActorSystem,implicit val ec:ExecutionContext,log:Logger) {
  import MSG_MinerData._
 
  val mqttHost = config.host
  val mqttPort = config.port
  val mqttConnectionSettings = MqttConnectionSettings(
    broker = s"tcp://${config.host}:${config.port}", 
    clientId = config.clientId, 
    persistence = new MemoryPersistence 
  ).withAutomaticReconnect(true)

  val mqttClientId = s"${config.clientId}}"
  val mqttConnectionId = s"${math.abs(Random.nextLong())}"
  val mqttTopic = config.topic

  val mqttSink: Sink[MqttMessage, Future[Done]] = MqttSink(mqttConnectionSettings, MqttQoS.AtLeastOnce)
    
  val mqttPublisher = Flow[MSG_MinerData].map( md => {
    
    val mqttData = upickle.default.writeBinary(md)
    val wireData = if(MSG_Options.isV1(config.protocolVer)) Util.hex(mqttData).getBytes else mqttData

    log.debug(s"(${Util.hex(mqttData)}) -> MQTT(${mqttHost}:${mqttPort})")
    MqttMessage(config.topic,ByteString(wireData))  
  })
    
  def flow() = mqttPublisher
  def sink() = mqttSink
}
