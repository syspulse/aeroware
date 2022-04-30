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

class MQTTClientSubscriber(config:MQTTConfig)(implicit val as:ActorSystem,implicit val ec:ExecutionContext,log:Logger) {
  import MSG_MinerData._
 
  val mqttHost = config.host
  val mqttPort = config.port
  val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
  val mqttSession = ActorMqttClientSession(mqttSettings)
  val mqttConnection = Tcp().outgoingConnection(mqttHost, mqttPort)
  val mqttClientId = s"${config.clientId}}"
  val mqttConnectionId = s"${math.abs(Random.nextLong())}"
  val mqttTopic = config.topic

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
          log.info(s"event: ${p}: ${p.payload.utf8String}")
          p
        }
      }
      //.wireTap(event => log.info(s"Client: ${mqttConnectionId}: event=${event}"))
      .log(s"MQTT(${mqttHost}:${mqttPort}): ")
      //.async
      .toMat(Sink.ignore)(Keep.both)
      .run()

  log.info(s"mqttQueue=${mqttQueue}, events=${events}")
  mqttQueue.offer(Command(Connect(mqttClientId, ConnectFlags.CleanSession)))
  log.info(s"mqttQueue=${mqttQueue}, events=${events}")
  mqttQueue.offer(Command(Subscribe(Seq((mqttTopic, ControlPacketFlags.QoSAtMostOnceDelivery)))))
  //mqttQueue.offer(Command(Subscribe(mqttTopic)))
    
  def flow() = events
}
