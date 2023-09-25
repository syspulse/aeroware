package io.syspulse.aeroware.adsb.radar.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection

import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

//import scala.collection.mutable.TreeMap
import scala.collection.mutable
import scala.concurrent.Future
import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.radar.Craft
import io.syspulse.aeroware.adsb.core.AircraftAddress
import io.syspulse.aeroware.adsb.mesh.protocol.MeshData
import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.Decoder
import io.syspulse.aeroware.adsb.core.ADSB_AirbornePositionBaro
import io.syspulse.aeroware.adsb.core.ADSB_AirborneVelocity
import io.syspulse.aeroware.adsb.radar.Radar
import io.syspulse.aeroware.adsb.radar.TrackTelemetry
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

class RadarStoreMem extends RadarStore {
  //implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val ec: scala.concurrent.ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  val log = Logger(s"${this}")
    
  val storeAddr: mutable.Map[String,mutable.Seq[TrackTelemetry]] = mutable.HashMap()
  val storeTs: mutable.TreeMap[Long,mutable.Seq[TrackTelemetry]] = mutable.TreeMap()
    
  val radar = new Radar()

  def all:Future[Try[Seq[TrackTelemetry]]] = {
    Future{
      Success(storeAddr.values.reduce(_ ++ _).toSeq)    
    }
  }

  def size:Long = storeAddr.values.foldLeft(0)(_ + _.size)

  def +(t:TrackTelemetry):Future[Try[RadarStoreMem]] = { 
    log.info(s"add: ${t}")
    Future { 
      storeAddr.put(t.aid.getKey(),
        storeAddr.getOrElseUpdate(
          t.aid.getKey(), mutable.ArrayBuffer()
        ).appended(t)
      )
      storeTs.put(t.ts,
        storeTs.getOrElseUpdate(t.ts, mutable.ArrayBuffer()).appended(t)
      )
      Success(this)
    }
  }

  def ??(addr:AircraftAddress,ts0:Long,ts1:Long):Future[Try[Seq[TrackTelemetry]]] = Future {
    Success(storeTs.range(ts0,ts1+1).values.flatten.toSeq)
  }

  def ?(addr:AircraftAddress):Future[Try[Seq[TrackTelemetry]]] = Future { 
    storeAddr.get(addr.getKey()) match {
      case Some(dd) => Success(dd.toSeq)
      case None => Failure(new Exception(s"not found: ${addr}"))
    }
  }

  def <--(d:MeshData):Future[Try[RadarStoreMem]] = {
    Future {
      val t = radar.signal(d.ts,d.data)
      t match {
        case Some(t) =>
          // save to datastore
          this.+(t)

        case None =>
      }
      Success(this)
    }
  }

}
