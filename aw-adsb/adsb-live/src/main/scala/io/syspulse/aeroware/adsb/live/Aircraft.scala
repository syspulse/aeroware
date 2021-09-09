package io.syspulse.aeroware.adsb.live

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

abstract class Aircraft(id:AircraftAddress) {
  def getId:AircraftAddress = id

  var telemetry:List[AircraftTelemetry] = List()

  def putTelemetry(t:AircraftTelemetry) = {telemetry = telemetry :+ t}
  def getTelemetry = telemetry.lastOption
  
}

case class AircraftFlying(id:AircraftAddress) extends Aircraft(id) {

}

case class AircraftGround(id:AircraftAddress) extends Aircraft(id) {

}

