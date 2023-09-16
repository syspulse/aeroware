package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import scala.collection.mutable

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.core.{Speed,VRate,Altitude,Location}
import io.syspulse.aeroware.adsb.core._


abstract class Trackable(id:AircraftAddress, cacheSize:Int = 100) {
  def getId:AircraftAddress = id
  
  val eventsCache:mutable.Stack[ADSB] = mutable.Stack()
  
  var pLast:Option[ADSB_AirbornePositionBaro] = None
  var p0:Option[ADSB_AirbornePositionBaro] = None

  var telemetry:List[TrackTelemetry] = List()
  val tFirst = TrackTelemetry(0L,id,Location.UNKNOWN,Speed.UNKNOWN,VRate.UNKNOWN,0.0)
  
  @volatile
  var tLast:Option[TrackTelemetry] = None

  var statTotalEvents:Long = 0L

  def last = tLast

  def event(a:ADSB):Option[TrackTelemetry] = {
    statTotalEvents = statTotalEvents + 1

    if(eventsCache.size > cacheSize) {
      eventsCache.dropRightInPlace(1)
    }
    eventsCache.push(a)

    val r = a match {
      case p:ADSB_AirbornePositionBaro => {
        if(p0.isDefined) {
          val loc = Decoder.getGloballPosition(p0.get,p)
          p0 = Some(p)

          val (hSpeed:Speed,vRate:VRate,heading:Double) = { 
            val t=tLast.getOrElse(tFirst)
            (t.hSpeed,t.vRate,t.heading)
          }

          val t = TrackTelemetry(p.ts,a.addr,loc,hSpeed,vRate,heading)
          
          telemetry = telemetry :+ t

          tLast = Some(t)
          tLast

        } else {
          p0 = Some(p)
          pLast = p0
          tLast
        }
      }
      case p:ADSB_AirborneVelocity => {
        val hSpeed = p.hSpeed
        val vRate = p.vRate
        val heading = p.heading
        val (loc:Location) = { 
          val t=tLast.getOrElse(tFirst)
          (t.loc)
        }
        
        val t = TrackTelemetry(p.ts,a.addr,loc,hSpeed,vRate,heading)
        telemetry = telemetry :+ t

        tLast = Some(t)
        tLast
      }

      case _ => {
        // ignore other events
        None
      }
    }

    r
  }

  def putTelemetry(t:TrackTelemetry) = {telemetry = telemetry :+ t}
  def getTelemetry = telemetry.lastOption
  
}


