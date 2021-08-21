package io.syspulse.aeroware.adsb.ingest

import java.nio.file.{Path,Paths, Files}

import scala.util.{Try,Failure,Success}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl.LogRotatorSink

import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format._

import java.net.InetSocketAddress

import scopt.OParser

import upickle._

import io.prometheus.client.Counter

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import akka.NotUsed


class ADSB_Ingest extends IngestClient {
  
  val metric_Total: Counter = Counter.build().name("aw_adsb_total").help("Total ADSB events").register()
  val metric_Err: Counter = Counter.build().name("aw_adsb_err").help("Total ADSB Errors events").register()
  val metric_Catch: Counter = Counter.build().name("aw_adsb_catch").help("Total Caught ADSB events").register()
  val metric_ADSB_Message: Map[String,Counter] = Seq(
    ADSB_Unknown.getClass().getSimpleName(),
    ADSB_AircraftIdentification.getClass().getSimpleName(),
    ADSB_SurfacePosition.getClass().getSimpleName(),
    ADSB_AirbornePositionBaro.getClass().getSimpleName(),
    ADSB_AirborneVelocity.getClass().getSimpleName(),
    ADSB_AirbornePositionGNSS.getClass().getSimpleName(),
    
    ADSB_Reserved.getClass().getSimpleName(),
    ADSB_AircraftStatus.getClass().getSimpleName(),
    ADSB_TargetState.getClass().getSimpleName(),
    ADSB_AircraftOperationStatus.getClass().getSimpleName()
  ).map(_.stripSuffix("$")).map( a =>
    a -> Counter.build().name(s"aw_adsb_${a}").help(s"Total ${a}").register()
  ).toMap
  
    def run(config:Config) = {
    
        implicit val system = ActorSystem("ADSB-Ingest")
        implicit val adsbRW = upickle.default.macroRW[ADSB_Event]

         val retrySettings = RestartSettings(
          minBackoff = 1.seconds,
          maxBackoff = 10.seconds,
          randomFactor = 0.1
        )//.withMaxRestarts(6, 1.minutes)

        val conn = InetSocketAddress.createUnresolved(config.dumpHost, config.dumpPort)
        val connection = Tcp().outgoingConnection(
          remoteAddress = conn,
          connectTimeout = Duration(config.connectTimeout,MILLISECONDS),
          idleTimeout = Duration(config.idleTimeout,MILLISECONDS)
        )

        val source =  Source.actorRef(1, OverflowStrategy.fail)
        val sourceRestarable = RestartSource.withBackoff(retrySettings) { () => 
          log.info(s"Connecting -> dump1090(${config.dumpHost}:${config.dumpPort})...")
          source
            .via(connection)
            .log("dump1090")
        }

        val f = DateTimeFormatter.ofPattern(config.filePattern)
        val lastTimestamp = System.currentTimeMillis()

        def getFileName() = {
          val suffix = ZonedDateTime.ofInstant(Instant.now, ZoneId.systemDefault).format(f)
          val outputFile = s"adsb-${suffix}.json"
          outputFile
        }
        val ver = 1

        val fileRotateTrigger: () => ByteString => Option[Path] = () => {
          var currentFilename: Option[String] = None
          var init = false
          val max = 10 * 1024 * 1024
          var count: Long = 0L
          var size: Long = 0L
          var currentTs = System.currentTimeMillis 
          (element: ByteString) => {
            if(init && (count < config.fileLimit && size < config.fileSize)) {
              count = count + 1
              size = size + element.size
              None
            } else {
              currentFilename = Some(getFileName())
              val outputPath = s"${config.dataDir}/${getFileName()}"
              log.info(s"Writing -> File(${outputPath})...")
              count = 0L
              size = 0L
              init = true
              Some(Paths.get(outputPath))
            }
          }
        }

        def decode(data:String):Option[ADSB] = {
          Decoder.decodeDump1090(data) match {
            case Success(a) => Some(a)
            case Failure(e) => None
          }
        }

        val sinkRestartable = if(config.filePattern!="NONE") { 
          RestartSink.withBackoff(retrySettings) { () =>
            LogRotatorSink(fileRotateTrigger)
          }
        } else Sink.ignore

        val framer = Flow[ByteString].via(Framing.delimiter(ByteString("\n"), 10000, allowTruncation = true))
        
        val converter = Flow[ByteString].map(v => { 
          metric_Total.inc()
          val r = decode(v.utf8String) 
          if(!r.isDefined) metric_Err.inc()
          r
        }).filter(_.isDefined).map(_.get)

        val filter = Flow[ADSB].filter(v => { 
          metric_ADSB_Message(v.getClass().getSimpleName()).inc
          v match {
            case a:ADSB_Unknown => false
            case _  => true
          }
        })

        val catcher = Flow[ADSB].map(v => {
          if(v.aircraftAddr.icaoType.matches(config.catchAircraft)) {
            metric_Catch.inc()
            println(s"Catch: ${Util.tsToString(v.ts)}: ${v}")
          }
          v
        })

        val transformer = Flow[ADSB].map(v => { 
          val ts = v.ts
          val `type` = v.getClass.getSimpleName
          val icaoId = v.aircraftAddr.icaoId
          val aircraft = v.aircraftAddr.icaoType
          val callsign = v.aircraftAddr.icaoCallsign
          
          ADSB_Event(ts, v.raw,`type`,icaoId,aircraft,callsign) 
        })
        
        val printer = Flow[ADSB_Event].map(v => { log.debug(s"${v}"); v }).log(s"output -> File(${getFileName()})")
        val jsoner = Flow[ADSB_Event].map(a => s"${upickle.default.write(a)}\n")

        val flow = RestartFlow.withBackoff(retrySettings) { () =>
          framer.via(converter).via(filter).via(catcher).via(transformer).via(printer).via(jsoner).map(ByteString(_)) 
        }

        sourceRestarable.via(flow).toMat(sinkRestartable)(Keep.both).run()

        //Await.result(futureFlow._3, Duration.Inf)
  
  }
}
