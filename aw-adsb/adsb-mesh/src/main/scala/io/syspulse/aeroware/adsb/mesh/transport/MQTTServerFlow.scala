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

import akka.stream.alpakka.mqtt.streaming.{MqttSessionSettings}
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
import scala.concurrent.Promise
import akka.Done
import akka.stream.alpakka.mqtt.streaming.ConnAck
import akka.stream.alpakka.mqtt.streaming.ConnAckReturnCode
import akka.stream.alpakka.mqtt.streaming.SubAck
import akka.stream.alpakka.mqtt.streaming.PubAck
import akka.stream.alpakka.mqtt.streaming.scaladsl.MqttServerSession
import akka.stream.alpakka.mqtt.streaming.ConnAckFlags
import scala.concurrent.ExecutionContext
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttServerSession

class MQTTServerFlow(config:MQTTConfig)(implicit val as:ActorSystem,ec:ExecutionContext,log:Logger) {
  
  import MSG_MinerData._
 
  val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
  val mqttSession = ActorMqttServerSession(mqttSettings)
  val mqttConnectionId = s"${config.clientId}- ${math.abs(Random.nextLong())}"
  val mqttTopc = config.topic
  val mqttMaxConnections = 1
  
  val bindSource: Source[Either[MqttCodec.DecodeError, Event[Nothing]], Future[Tcp.ServerBinding]] =
    Tcp()
    .bind(config.host, config.port)
    .flatMapMerge(
      mqttMaxConnections, { connection =>
        val mqttFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
          Mqtt
            .serverSessionFlow(mqttSession, ByteString(connection.remoteAddress.getAddress.getAddress))
            .join(connection.flow)

        val (queue, source) = Source
          .queue[Command[Nothing]](3, OverflowStrategy.dropHead)
          .via(mqttFlow)
          .toMat(BroadcastHub.sink)(Keep.both)
          .run()

        val subscribed = Promise[Done]
        source
          .runForeach {
            case Right(Event(_: Connect, _)) =>
              queue.offer(Command(ConnAck(ConnAckFlags.None, ConnAckReturnCode.ConnectionAccepted)))
            case Right(Event(cp: Subscribe, _)) =>
              queue.offer(Command(SubAck(cp.packetId, cp.topicFilters.map(_._2)), Some(subscribed), None))
            case Right(Event(publish @ Publish(flags, _, Some(packetId), _), _))
                if flags.contains(ControlPacketFlags.RETAIN) =>
              queue.offer(Command(PubAck(packetId)))
              subscribed.future.foreach(_ => mqttSession ! Command(publish))
            case _ => // Ignore everything else
          }

        source
      }
    )
  
  // val (bound: Future[Tcp.ServerBinding], server: UniqueKillSwitch) = bindSource
  //   .viaMat(KillSwitches.single)(Keep.both)
  //   .to(Sink.ignore)
  //   .run()

  // for shutting down properly
  //server.shutdown()
  //session.shutdown()


  val mqtt = bindSource
    .map(e => {
      println(s"${e}")
      e
    })
    
    
  def flow() = mqtt
}
