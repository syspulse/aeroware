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

class RadarStoreMem extends RadarStore {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val log = Logger(s"${this}")
  
  var storeAddr: mutable.Map[String,mutable.Seq[TrackTelemetry]] = mutable.HashMap()
  var storeTs: mutable.TreeMap[Long,mutable.Seq[TrackTelemetry]] = mutable.TreeMap()
  
  val radar = new Radar()

  def all:Future[Try[Seq[TrackTelemetry]]] = Future{ Success(storeAddr.values.reduce(_ ++ _).toSeq) }

  def size:Long = storeAddr.values.foldLeft(0)(_ + _.size)

  def +(t:TrackTelemetry):Future[Try[RadarStoreMem]] = Future { 
    storeAddr.getOrElseUpdate(t.aid.getKey(), mutable.Seq()).+:(t)
    storeTs.getOrElseUpdate(t.ts, mutable.Seq()).+:(t)      
    Success(this)
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
          this.+(t)

        case None =>
      }
      Success(this)
    }
  }

}
