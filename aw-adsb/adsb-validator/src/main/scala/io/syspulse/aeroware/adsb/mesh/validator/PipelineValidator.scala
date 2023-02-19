package io.syspulse.aeroware.adsb.mesh.validator

import scala.util.Random
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.javadsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

import akka.stream.scaladsl.Tcp

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit
import java.net.InetSocketAddress
import akka.stream.scaladsl.RestartSource
import akka.stream.OverflowStrategy

import scala.concurrent.Future
import scala.util.Random
import scala.util.Failure
import scala.util.Success
import akka.stream.scaladsl.BroadcastHub


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

import akka.stream.alpakka.mqtt.MqttMessage
import akka.Done
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.MqttQoS
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RestartSink
import akka.stream.alpakka.mqtt.MqttConnectionSettings

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import io.swagger.v3.oas.models.security.SecurityScheme.In


import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw

import io.syspulse.aeroware.adsb.ingest.Dump1090URI
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingested

import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.transport.MqttURI

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.{WalletVaultKeyfiles,WalletVaultKeyfile}

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerADSB
import io.syspulse.aeroware.adsb.mesh.protocol.MinerSig
import io.syspulse.aeroware.adsb.mesh.transport.MQTTServerFlow
import io.syspulse.aeroware.adsb.mesh.transport.MQTTConfig

import io.syspulse.aeroware.adsb.mesh.store._
import io.syspulse.aeroware.adsb.mesh.rewards._
import io.syspulse.aeroware.adsb.mesh.validator._
import io.syspulse.aeroware.adsb.mesh.validation._
import io.syspulse.aeroware.adsb.mesh._

import akka.NotUsed

case class PublishWithAddr (remoteAddr: InetSocketAddress,
                            flags: ControlPacketFlags,
                            topicName: String,
                            packetId: Option[PacketId],
                            payload: ByteString)

class PipelineValidator(feed:String,output:String,datastore:DataStore)(implicit config:Config,fmt:JsonFormat[MSG_MinerData])
  extends Pipeline[MSG_MinerData,MSG_MinerData,MSG_MinerData](feed,output,config.throttle,config.delimiter,config.buffer)(fmt) {
  implicit protected val log = Logger(s"${this}")
  //implicit val ec = system.dispatchers.lookup("default-executor") //ExecutionContext.global
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  // ----- Wallet ---
  val wallet = new WalletVaultKeyfile(config.keystore, config.keystorePass)  
  val wr = wallet.load()
  log.info(s"wallet: ${wr}")
  val signerPk = wallet.signers.toList.head._2.head.pk
  val signerAddr = wallet.signers.toList.head._2.head.addr

  // ----- Validation ---
  val validationEngine = new ValidationADSB()
   
  // MQTT Server (Broker)
  val connectTimeout = 1000L
  val idleTimeout = 15000L

  def asMQTT(mqttHost:String,mqttPort:Int,mqttTopic:String="adsb",clientId:String="adsb-client",protocolVer:Int = 0) = {
    
    val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
    val mqttSession = ActorMqttServerSession(mqttSettings)
    val mqttConnectionId = s"${clientId}- ${math.abs(Random.nextLong())}"
    
    // max connections are cumulative (not simultaneous). Disconnects do not decrement !
    val mqttMaxConnections = Int.MaxValue
    
    val bindSource = //: Source[Either[MqttCodec.DecodeError, Event[Nothing]], Future[Tcp.ServerBinding]] =
      Tcp()
      .bind(mqttHost, mqttPort, halfClose = true, idleTimeout = FiniteDuration(idleTimeout,TimeUnit.MILLISECONDS))
      //.idleTimeout(FiniteDuration(idleTimeout,TimeUnit.MILLISECONDS)) // <- This completes the Server binding ! (no client can connect)
      .flatMapMerge( mqttMaxConnections, { connection:Tcp.IncomingConnection =>
      // .map( connection => {
          log.info(s"Miner(${connection.remoteAddress}) ---> MQTT(${mqttHost}:${mqttPort})")
          val mqttConnectionFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
              Mqtt
                .serverSessionFlow(mqttSession, ByteString(connection.remoteAddress.getAddress.getAddress))                
                .join(
                  connection.flow.log(s"Miner(${connection.remoteAddress}) -> MQTT(${mqttHost}:${mqttPort})")
                  .watchTermination()( (v, f) => 
                    f.onComplete {
                      case Failure(err) => log.error(s"connection flow failed",err)
                      case Success(_) => log.warn(s"connection terminated: ${connection.remoteAddress}")
                  })
                )
                .idleTimeout(FiniteDuration(idleTimeout,TimeUnit.MILLISECONDS))  // disconnect idle client (emualted with telnet 127.0.0.1 1883)
            
          val (queue, source) = Source
            .queue[Command[Nothing]](3, OverflowStrategy.dropHead)
            .log(s"MQTT Command Queue")
            .via(mqttConnectionFlow)            
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

          val subscribed = Promise[Done]()
          source
            .map(r => {
              log.debug(s"${r} -> MQTT(${mqttHost}:${mqttPort})")
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
    
    bindSource
      .collect( mqtt => 
        mqtt match {
          case PublishWithAddr(remoteAddr,flags,topicName,packetId,payload) => {
          //case Right(Event(publish @ Publish(flags, topic, Some(packetId), payload), _)) => {
            
            // inject remote address into payload
            // EXCEPTIONALLY UNOPTIMIZED, I am just tired and want to have something working before sleep
            log.debug(s"Payload[${Util.hex(payload.toArray)}] <- MQTT(${remoteAddr})")
            ByteString(s"${remoteAddr.getAddress().getHostAddress()}:${remoteAddr.getPort().toString}/${Util.hex(payload.toArray)}")
          }
        }
      )
  }
  
  def filter:Seq[String] = config.filter
    
  override def source() = {
    feed.split("://").toList match {
      case "mqtt" :: _ => {
        val uri = MqttURI(feed)
        asMQTT(uri.host,uri.port.toInt)
      }
      case _ => super.source()
    }
  }

  override def process = Flow[MSG_MinerData].map( m => m)

  def parse(data:String):Seq[MSG_MinerData] = {    
    log.debug(s"data: ${data}")
    val remoteAddr = data.takeWhile(_ != '/')
    val payload = data.dropWhile(_ != '/').drop(1)
        
    val wireData = ByteString(Util.fromHexString(payload))
    log.debug(s"encoded: ${Util.hex(wireData.toArray)}")

    val encodedData = if(MSG_Options.isV1(config.protocolOptions)) Util.fromHexString(wireData.utf8String) else wireData.toArray
    val msg = upickle.default.readBinary[MSG_MinerData](encodedData)
    msg.copy(socket = remoteAddr)
    val m = msg

    // fast validation path to prevent Spam
    val r1 = validationEngine.validate(m)

    if(r1 > 0.0)
      datastore.+(m)
  
    Seq(m)
  }

  def transform(a: MSG_MinerData): Seq[MSG_MinerData] = Seq(a)  
}
