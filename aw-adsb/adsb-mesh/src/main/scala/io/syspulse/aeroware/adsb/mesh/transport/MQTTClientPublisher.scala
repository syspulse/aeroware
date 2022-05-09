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

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.ingest.AdsbIngest

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import scala.concurrent.ExecutionContext
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Version
import io.swagger.v3.oas.models.security.SecurityScheme.In
import java.net.InetSocketAddress

class MQTTClientPublisher(config:MQTTConfig)(implicit val as:ActorSystem,implicit val ec:ExecutionContext,log:Logger) {
  import MSG_MinerData._
 
  val mqttHost = config.host
  val mqttPort = config.port
  val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
  val mqttSession = ActorMqttClientSession(mqttSettings)
  val mqttClientId = s"${config.clientId}}"
  val mqttConnectionId = s"${math.abs(Random.nextLong())}"
  val mqttTopic = config.topic

  val mqttConnection = Tcp().outgoingConnection(
    remoteAddress = new InetSocketAddress(mqttHost, mqttPort),
    connectTimeout = Duration("3 seconds"),
    idleTimeout = Duration("10 seconds")
  )

  val restartSettings = RestartSettings(1.second, 5.seconds, 0.2)//.withMaxRestarts(10, 1.minute)
  val restartFlowTcp = RestartFlow.onFailuresWithBackoff(restartSettings)(() => mqttConnection)
  val restartFlowMQTT = RestartFlow.onFailuresWithBackoff(restartSettings)(() => mqttFlow)

  val mqttFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
    Mqtt
      .clientSessionFlow(mqttSession, ByteString(mqttConnectionId))
      .join(restartFlowTcp)

  val (mqttQueue: SourceQueueWithComplete[Command[Nothing]], events: Future[Publish]) =
    Source
      .queue(10, OverflowStrategy.fail)
      .via(restartFlowMQTT)
      .collect {
        case Right(Event(p: Publish, _)) => {
          log.debug(s"${p}")
          p
        }
      }
      .log(s"MQTT(${mqttHost}:${mqttPort}): ")
      //.async
      .toMat(Sink.head)(Keep.both)
      .run()

  mqttQueue.offer(Command(Connect(mqttClientId, ConnectFlags.CleanSession)))
  
  val mqttPublisher = Flow[MSG_MinerData].map( md => {
    
    val mqttData = upickle.default.writeBinary(md)
    val wireData = if(config.protocolVer == MSG_Version.V1) Util.hex(mqttData).getBytes else mqttData

    log.debug(s"(${Util.hex(mqttData)}) -> MQTT(${mqttHost}:${mqttPort})")
    mqttSession ! Command(
      Publish(ControlPacketFlags.QoSAtLeastOnceDelivery, mqttTopic, ByteString(wireData))
    )
    
    mqttData
  })
    
  def flow() = mqttPublisher
}
