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
import scala.util.Random
import scala.concurrent.Future

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import java.net.InetSocketAddress
import akka.stream.alpakka.mqtt.streaming.{MqttSessionSettings}
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttClientSession
import akka.stream.alpakka.mqtt.streaming.scaladsl.Mqtt
import akka.stream.alpakka.mqtt.streaming.MqttCodec
import akka.stream.alpakka.mqtt.streaming.Event
import akka.stream.alpakka.mqtt.streaming.Publish
import akka.stream.alpakka.mqtt.streaming.Command
import akka.stream.alpakka.mqtt.streaming.Connect
import akka.stream.alpakka.mqtt.streaming.Subscribe
import akka.stream.alpakka.mqtt.streaming.ConnectFlags
import akka.stream.alpakka.mqtt.streaming.ControlPacketFlags
import akka.stream.alpakka.mqtt.streaming.ConnAck
import akka.stream.alpakka.mqtt.streaming.ConnAckReturnCode
import akka.stream.alpakka.mqtt.streaming.SubAck
import akka.stream.alpakka.mqtt.streaming.PubAck
import akka.stream.alpakka.mqtt.streaming.scaladsl.MqttServerSession
import akka.stream.alpakka.mqtt.streaming.ConnAckFlags
import akka.stream.alpakka.mqtt.streaming.scaladsl.ActorMqttServerSession
import akka.stream.alpakka.mqtt.streaming.PacketId
import akka.stream.alpakka.mqtt.streaming.ControlPacket
import akka.stream.alpakka.mqtt.streaming.ControlPacketType

import io.syspulse.aeroware.core.Raw
import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import scala.concurrent.Promise
import akka.Done
import scala.concurrent.ExecutionContext

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Options



case class PublishWithAddr (addr: InetSocketAddress,
                            flags: ControlPacketFlags,
                            topicName: String,
                            packetId: Option[PacketId],
                            payload: ByteString)

class MQTTServerFlow(config:MQTTConfig)(implicit val as:ActorSystem,ec:ExecutionContext,log:Logger) {
  
  import MSG_MinerData._
 
  val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
  val mqttSession = ActorMqttServerSession(mqttSettings)
  val mqttConnectionId = s"${config.clientId}- ${math.abs(Random.nextLong())}"
  val mqttTopc = config.topic
  val mqttMaxConnections = 2
  
  val bindSource = //: Source[Either[MqttCodec.DecodeError, Event[Nothing]], Future[Tcp.ServerBinding]] =
    Tcp()
    .bind(config.host, config.port, halfClose = false, idleTimeout = Duration("10 seconds"))
    .flatMapMerge(
      mqttMaxConnections, { connection:Tcp.IncomingConnection =>
    // .map( connection => {
        log.info(s"Miner(${connection.remoteAddress}) ---> MQTT(${config.host}:${config.port})")
        val mqttConnectionFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
            Mqtt
              .serverSessionFlow(mqttSession, ByteString(connection.remoteAddress.getAddress.getAddress))
              .join(
                connection.flow.log(s"Miner(${connection.remoteAddress}) ? -> MQTT(${config.host}:${config.port})")
                .watchTermination()( (v, f) => 
                  f.onComplete {
                    case Failure(err) => log.error(s"connection flow failed: $err")
                    case Success(_) => log.warn(s"connection terminated: client: ${connection.remoteAddress}")
                })
              )      
          
        val (queue, source) = Source
          .queue[Command[Nothing]](3, OverflowStrategy.dropHead)
          .via(mqttConnectionFlow)
          .log(s"MQTT Command Queue")
          .toMat(BroadcastHub.sink)(Keep.both)
          .run()
        

        // very unoptimial way to work around types
        // Publish Event is sealed and I cannot pass connection address from source (which is Event typed)
        // downstream
        // This queue is a type-decoupling
        val (queueOut, sourceOut) = Source
          .queue[PublishWithAddr](3, OverflowStrategy.dropHead)
          .toMat(BroadcastHub.sink)(Keep.both)
          .run()

        val subscribed = Promise[Done]
        source
          .map(r => {
            log.debug(s"${r} -> MQTT(${config.host}:${config.port})")
            r
          })
          .map {
            case Right(Event(_: Connect, _)) =>
              queue.offer(Command(ConnAck(ConnAckFlags.None, ConnAckReturnCode.ConnectionAccepted)))
                            
            case Right(Event(cp: Subscribe, _)) =>
              queue.offer(Command(SubAck(cp.packetId, cp.topicFilters.map(_._2)), Some(subscribed), None))
              
            case Right(Event(publish @ Publish(flags, topic, Some(packetId), payload), _))
                //if flags.contains(ControlPacketFlags.RETAIN) =>
                => 
            
              queue.offer(Command(PubAck(packetId)))
              subscribed.future.foreach(_ => mqttSession ! Command(publish))

              queueOut.offer(
                PublishWithAddr(
                    connection.remoteAddress,
                    publish.flags,
                    publish.topicName,
                    publish.packetId,
                    publish.payload
              ))
              
             case _ => // Ignore everything else
          }
          .run()  

        sourceOut
      }
    )
  
  // val (bound: Future[Tcp.ServerBinding], server: UniqueKillSwitch) = bindSource
  //   .viaMat(KillSwitches.single)(Keep.both)
  //   .to(Sink.ignore)
  //   .run()

  // for shutting down properly
  //server.shutdown()
  //session.shutdown()
  
  // def source() = RestartSource.onFailuresWithBackoff(RestartSettings(1.seconds,3.seconds,0.2)) { 
  //   () => bindSource
  // }

  def source() = bindSource


  def flow() = source()
    .collect( mqtt => 
      mqtt match {
        case PublishWithAddr(remoteAddr,flags,topicName,packetId,payload) => {
        //case Right(Event(publish @ Publish(flags, topic, Some(packetId), payload), _)) => {
          val wireData = payload
          log.debug(s"mqtt: ${Util.hex(wireData.toArray)}")
          val data = if(MSG_Options.isV1(config.protocolVer)) Util.fromHexString(wireData.utf8String) else wireData.toArray
          val msg = upickle.default.readBinary[MSG_MinerData](data)
          msg.copy(socket = remoteAddr.toString)
        }
      }
    )
}
