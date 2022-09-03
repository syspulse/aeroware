package io.syspulse.aeroware.adsb.ingest.old

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
import scala.concurrent.ExecutionContext


class AdsbIngest extends IngestClient {
  
  val metric_Total: Counter = Counter.build().name("aw_adsb_total").help("Total ADSB events").register()
  val metric_Err: Counter = Counter.build().name("aw_adsb_err").help("Total ADSB Errors events").register()
  val metric_track: Counter = Counter.build().name("aw_adsb_track").help("Total Caught ADSB events").register()
  
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

  def logFlow = Flow[ADSB].map(v => { 
    val ts = v.ts
    val `type` = v.getClass.getSimpleName
    val icaoId = v.aircraftAddr.icaoId
    val aircraft = v.aircraftAddr.icaoType
    val callsign = v.aircraftAddr.icaoCallsign
    
    ADSB_Log(ts, v.raw,`type`,icaoId,aircraft,callsign) 
  })
  
  def debugFlow = Flow[ADSB].map(v => { 
    log.debug(s"${v}")
    v 
  })
  
  def flow(config:Config):Source[ADSB,_] = {
    
    //implicit val system = ActorSystem("ADSB-Ingest")
    
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

    val sourceRestarable = RestartSource.withBackoff(retrySettings) { () => 
      log.info(s"Connecting -> dump1090(${config.dumpHost}:${config.dumpPort})...")
      Source.actorRef(1, OverflowStrategy.fail)
        .via(connection)
        .log("dump1090")
    }

    def decode(data:String):Option[ADSB] = {
      Decoder.decodeDump1090(data) match {
        case Success(a) => Some(a)
        case Failure(e) => None
      }
    }

    def framer = Flow[ByteString].via(Framing.delimiter(ByteString("\n"), 10000, allowTruncation = true))
    
    def converter = Flow[ByteString].map(v => { 
      metric_Total.inc()
      val r = decode(v.utf8String) 
      if(!r.isDefined) metric_Err.inc()
      r
    }).filter(_.isDefined).map(_.get)

    def filter = Flow[ADSB].filter(v => { 
      metric_ADSB_Message(v.getClass().getSimpleName()).inc
      v match {
        case a:ADSB_Unknown => false
        case _  => true
      }
    })

    // Very unoptimized tracker
    def tracker = Flow[ADSB].filter(v => {
      if(config.trackAircraft == "" ||
          v.aircraftAddr.icaoType.matches(config.trackAircraft) ||
          v.aircraftAddr.icaoCallsign.matches(config.trackAircraft) ||
          (v.isInstanceOf[ADSB_AircraftIdentification] && v.asInstanceOf[ADSB_AircraftIdentification].callSign.matches(config.trackAircraft) ||
          v.aircraftAddr.icaoId.matches(config.trackAircraft))
      ) {
        metric_track.inc()
        //println(s"${Util.tsToString(v.ts)}: ${v}")
        true
      } else false
    })

    // customizer empty mapper is overkill
    val source = RestartFlow.withBackoff(retrySettings) { () =>
      framer
      .via(converter)
      .via(filter)
      .via(tracker)
      //.via(transformer.getOrElse(logTransformer))
      .via(debugFlow)
      // .via(format)
      // .map(ByteString(_))
    }

    sourceRestarable.via(source)

  }
}
