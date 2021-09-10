package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import scala.collection.mutable

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.core.{Speed,VRate,Altitude,Location}
import io.syspulse.aeroware.adsb.core._

abstract class Trackable(id:AircraftAddress, cacheSize:Int = 100) {
  def getId:AircraftAddress = id

  def status = "flying"
  def callSign = id.icaoCallsign

  val eventsCache:mutable.Stack[ADSB] = mutable.Stack()
  
  var pLast:Option[ADSB_AirbornePositionBaro] = None
  var p0:Option[ADSB_AirbornePositionBaro] = None

  var telemetry:List[AircraftTelemetry] = List()
  val tFirst = AircraftTelemetry(id,Location.UNKNOWN,Speed.UNKNOWN,VRate.UNKNOWN,0.0)
  @volatile
  var tLast:Option[AircraftTelemetry] = None

  var statTotalEvents:Long = 0L

  def last = tLast

  def event(a:ADSB):Trackable = {
    statTotalEvents = statTotalEvents + 1

    if(eventsCache.size > cacheSize) 
      eventsCache.dropRightInPlace(1)
    eventsCache.push(a)


    a match {
      case p:ADSB_AirbornePositionBaro => {
        if(p0.isDefined) {
          val loc = Decoder.getGloballPosition(p0.get,p)
          p0 = Some(p)

          val (hSpeed:Speed,vRate:VRate,heading:Double) = { val t=tLast.getOrElse(tFirst); (t.hSpeed,t.vRate,t.heading)}
          val t = AircraftTelemetry(a.aircraftAddr,loc,hSpeed,vRate,heading)
          telemetry = telemetry :+ t

          tLast = Some(t)

        } else {
          p0 = Some(p)
          pLast = p0
        }
      }
      case p:ADSB_AirborneVelocity => {
        val hSpeed = p.hSpeed
        val vRate = p.vRate
        val heading = p.heading
        val (loc:Location) = { val t=tLast.getOrElse(tFirst); (t.loc)}
        val t = AircraftTelemetry(a.aircraftAddr,loc,hSpeed,vRate,heading)
        telemetry = telemetry :+ t

        tLast = Some(t)
      }

      case _ => {
        // ignore other events
      }
    }

    this
  }

  def putTelemetry(t:AircraftTelemetry) = {telemetry = telemetry :+ t}
  def getTelemetry = telemetry.lastOption
  
}

case class Aircraft(id:AircraftAddress) extends Trackable(id) {

}


