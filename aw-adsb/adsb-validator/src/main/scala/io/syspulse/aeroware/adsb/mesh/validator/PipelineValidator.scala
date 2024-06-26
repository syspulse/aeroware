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
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RestartSink
import akka.Done

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

import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.transport.MqttURI

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.{WalletVaultKeyfiles,WalletVaultKeyfile}

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerPayload
import io.syspulse.aeroware.adsb.mesh.protocol.MinerSig
import io.syspulse.aeroware.adsb.mesh.transport.MQTTConfig

import io.syspulse.aeroware.adsb.mesh.rewards._
import io.syspulse.aeroware.adsb.mesh.validator._
import io.syspulse.aeroware.adsb.mesh._

import akka.NotUsed
import java.util.concurrent.atomic.AtomicLong

import io.syspulse.aeroware.adsb.mesh.store.MinedStore
import io.syspulse.aeroware.adsb.mesh.guard.GuardSpam

case class AddrStat(total:AtomicLong = new AtomicLong(),errors:AtomicLong = new AtomicLong()) {
  override def toString = s"${total.get()},${errors.get()}"
}

class ValidatorStat {
  val total = new AtomicLong()
  val errors = new AtomicLong()
  var addrs = Map[String,AddrStat]()
  
  def +(addr:Array[Byte],sz:Long,err:Long):Long = {
    val a = Util.hex(addr)
    val as = addrs.getOrElse(a,AddrStat())

    if(err != 0) {
      errors.addAndGet(err)
      as.errors.addAndGet(err)
    }
    total.addAndGet(sz)
    as.total.addAndGet(sz)
    addrs = addrs + (a -> as)
  
    total.get()
  }

  override def toString = {
    s"${total},${errors}: "+
    addrs.take(10).map{ case(addr,as) => s"[${addr}:(${as.toString})"}.mkString(",")
  }
}

case class PublishWithAddr (remoteAddr: InetSocketAddress,
                            flags: ControlPacketFlags,
                            topicName: String,
                            packetId: Option[PacketId],
                            payload: ByteString)

class PipelineValidator(feed:String,output:String,storeMined:MinedStore)(implicit config:Config)
  extends Pipeline[MSG_MinerData,MeshData,MeshData](feed,output,config.throttle,config.delimiter,config.buffer) {
  
  implicit protected val log = Logger(s"${this}")
  //implicit val ec = system.dispatchers.lookup("default-executor") //ExecutionContext.global
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val validatorStat = new ValidatorStat()

  // ----- Wallet ---
  val wallet = new WalletVaultKeyfile(config.keystore, config.keystorePass)  
  val wr = wallet.load()
  
  val validatorAddr = wallet.signers.values.toList.head.addr
  
  val configValidator = ValidatorConfig (
        validateTs = config.validation.contains("ts"),
        validateSig = config.validation.contains("sig"),
        validateData = config.validation.contains("data"),
        validatePayload = config.validation.contains("payload"),
        toleranceTs = config.toleranceTs,

        validateAddrBlacklist = config.validation.contains("blacklist") || config.validation.contains("blacklist.addr"),
        validateIpBlacklist = config.validation.contains("blacklist.ip"),
        blacklistAddr = config.blacklistAddr,
        blacklistIp = config.blacklistIp,
    )

  // --- Stream Validators --------------------------------------------------
  // Additional validation is performed in batch mode to detect complex frauds
  // and correctly distribute rewards for the same data
  val guardSpam = GuardSpam(expire = config.spamExpire)

  val validator = (config.entity match {
    case "adsb" => new ValidatorADSB(configValidator)
    case "notam" => new ValidatorNOTAM(configValidator)
    case "any" | "aeroware" | "" => new ValidatorAny(configValidator)
    case _ => 
      log.error(s"unknown entity: ${config.entity}")
      sys.exit(2)
  }).add(guardSpam)
   
  // MQTT Server (Broker)
  val connectTimeout = config.timeoutConnect
  val idleTimeout = config.timeoutIdle

  def asMQTT(mqttHost:String,mqttPort:Int,
    //mqttTopic:String="adsb",
    mqttTopic:String=config.entity,
    clientId:String="aw-miner",
    protocolVer:Int = 0) = {
    
    val mqttSettings = MqttSessionSettings().withMaxPacketSize(8192)
    val mqttSession = ActorMqttServerSession(mqttSettings)
    //val mqttConnectionId = s"${clientId}- ${math.abs(Random.nextLong())}"
    
    // max connections are cumulative (not simultaneous). Disconnects do not decrement !
    val mqttMaxConnections = Int.MaxValue
    
    val bindSource = //: Source[Either[MqttCodec.DecodeError, Event[Nothing]], Future[Tcp.ServerBinding]] =
      Tcp()
      .bind(mqttHost, mqttPort, halfClose = true, idleTimeout = FiniteDuration(idleTimeout,TimeUnit.MILLISECONDS))
      //.idleTimeout(FiniteDuration(idleTimeout,TimeUnit.MILLISECONDS)) // <- This completes the Server binding ! (no client can connect)
      .flatMapMerge( mqttMaxConnections, { connection:Tcp.IncomingConnection =>
      // .map( connection => {
          val mqttConnectionId = connection.remoteAddress.toString
          //val mqttConnectionId = s"${clientId}-${math.abs(Random.nextLong())}"
          log.info(s"mqtt://${mqttHost}:${mqttPort}/${mqttTopic} <-- Miner(${connection.remoteAddress})")
          val mqttConnectionFlow: Flow[Command[Nothing], Either[MqttCodec.DecodeError, Event[Nothing]], NotUsed] =
              Mqtt
                .serverSessionFlow(mqttSession, ByteString(mqttConnectionId))                
                .join(
                  connection.flow.log(s"mqtt://${mqttHost}:${mqttPort}/${mqttTopic} <-- Miner(${connection.remoteAddress})")
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
              log.debug(s"mqtt://${mqttHost}:${mqttPort}/${mqttTopic} <- ${r}")
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
            log.debug(s"<- mqtt://${remoteAddr}(Payload[${Util.hex(payload.toArray)}])")
            //ByteString(s"${remoteAddr.getAddress().getHostAddress()}:${remoteAddr.getPort().toString}/${Util.hex(payload.toArray)}")
            
            val remoteAddressFull = s"${remoteAddr.getAddress().getHostAddress()}:${remoteAddr.getPort().toString}"
            val addressMarker = s"ADDR,${"%02d".format(remoteAddressFull.size)},${remoteAddressFull}/"
            //val output = ByteString(addressMarker).++(payload)
            val output = ByteString(addressMarker + Util.hex(payload.toArray))
            output
          }
        }
      )
  }
    
  override def source() = {
    feed.split("://").toList match {
      case "mqtt" :: _ => {
        val uri = MqttURI(feed)
        asMQTT(uri.host,uri.port.toInt)
      }
      case _ => super.source()
    }
  }

  override def process = Flow[MSG_MinerData].filter( m => {
    // fast validation path to prevent Spam
    val penalty = validator.validate(m)

    // non-blocking
    storeMined.+(m,penalty)

    val err = if(penalty < 0.0) {      
      log.warn(s"penalty: ${penalty}: ${Util.hex(m.addr)}")
      m.payload.size
    } else {
      0
    }
    
    validatorStat.+(m.addr,m.payload.size,err)

    log.info(s"stat=[${m.payload.size},${err},${validatorStat}]")

    err == 0
  })
  .mapConcat( m => {
    m.payload.map(a => 
      MeshData(
        ts = a.ts,
        pt = a.pt,
        data = a.data
    )).toSeq
  })
  .groupedWithin(Int.MaxValue,FiniteDuration(config.fanoutWindow,TimeUnit.MILLISECONDS))
  // .mapConcat( group => {
  //   // sort by timestamp and remove duplicates
  //   // there is always a possibility that duplicate can be in another window at the window edge
  //   // |         A(10,"MSG") | A(11,"MSG")         |
  //   group
  //     .sortBy(_.ts)
  //     .distinctBy( f => f.data)
  // })
  .statefulMapConcat { () =>
      var state = List.empty[MeshData]
      var lastTs = System.currentTimeMillis()
      (mm) => {
        //val currentWindowStart = eventTime - windowDuration.toMillis
        val uniq = mm
          .filter(m => ! state.find(_.data == m.data).isDefined)
          .sortBy(_.ts)
          
        state =  state.prependedAll( uniq )
        val now = System.currentTimeMillis()
        // Two window tolerances for duplication (rest will be skipped on prevalidation)
        if( (now - lastTs) > config.fanoutDedup) {
          // take only latest messages
          state = state.takeWhile(m => m.ts > lastTs)
          lastTs = now
        }

        log.debug(s"dedup: ${uniq} (state=${state})")
        uniq
      }
    }

  def parse(data:String):Seq[MSG_MinerData] = {    
    log.debug(s"data: ${data}")
    
    val (remoteAddr,payload) = {
      // check if payload contains injected address
      if(data.take(4) == "ADDR") {
        //get size
        val sz = data.drop(4 + 1).take(2).toInt

        val remoteAddr = data.drop(4 + 1 + 2 + 1).take(sz)
        val payload = data.drop(4 + 1 + 2 + 1 + sz + 1)
        (remoteAddr,payload)
      } else 
        ("",data)
    }
        
    log.debug(s"${remoteAddr}: ${payload}")

    // protection before parsing and working with data
    if(validator.allow(remoteAddr) < 0.0 ) {
      log.warn(s"block: ${remoteAddr}")
      return Seq()
    }
    
    val wireData = ByteString(Util.fromHexString(payload))
    // log.debug(s"encoded: ${Util.hex(wireData.toArray)}")
    val encodedData = if(MSG_Options.isV1(config.protocolOptions)) Util.fromHexString(wireData.utf8String) else wireData.toArray
    //val encodedData = if(MSG_Options.isV1(config.protocolOptions)) Util.fromHexString(payload) else payload.getBytes()
    
    log.debug(s"encoded: ${encodedData}")
    
    val msgs = try {
      val msg = upickle.default.readBinary[MSG_MinerData](encodedData)
      Seq(
        msg.copy(socket = remoteAddr)
      )
    } catch {
      case e:Exception => 
        log.warn(s"failed to parse from ${remoteAddr}: ${Util.hex(encodedData)}: ${e.getMessage()}")
        // prevent Spam by collecting InetAddress db automatic throttling
        guardSpam.add(remoteAddr)
        Seq()
    }
    
    msgs
  }

  def transform(d: MeshData): Seq[MeshData] = Seq(d)  
}
