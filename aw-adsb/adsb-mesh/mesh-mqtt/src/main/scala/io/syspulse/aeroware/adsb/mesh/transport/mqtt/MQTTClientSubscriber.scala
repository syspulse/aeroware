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

import io.syspulse.aeroware.core.Raw
import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import scala.concurrent.ExecutionContext
import io.syspulse.aeroware.adsb.mesh.protocol.MinerSig
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Options
import java.net.InetSocketAddress
import akka.stream.alpakka.mqtt.MqttConnectionSettings
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import akka.stream.alpakka.mqtt.MqttMessage
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.Done
import akka.stream.alpakka.mqtt.MqttSubscriptions
import akka.stream.alpakka.mqtt.MqttQoS

class MQTTClientSubscriber(config:MQTTConfig)(implicit val as:ActorSystem,implicit val ec:ExecutionContext,log:Logger) {
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

  val mqttSource: Source[MqttMessage, Future[Done]] = MqttSource.atMostOnce(
    mqttConnectionSettings,
    MqttSubscriptions(Map(config.topic -> MqttQoS.AtLeastOnce)),
    bufferSize = 8)

  def mqttSubscriber = Flow[MqttMessage].map(mqtt => {
    val wireData = mqtt.payload
    log.debug(s"mqtt: ${Util.hex(wireData.toArray)}")
 
    val data = if(MSG_Options.isV1(config.protocolVer)) Util.fromHexString(wireData.utf8String) else wireData.toArray
    val msg = upickle.default.readBinary[MSG_MinerData](data)
    msg
  })

  
  def source() = mqttSource
}
