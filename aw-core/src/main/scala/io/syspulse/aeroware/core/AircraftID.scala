package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

case class AircraftID(icaoId:String,callsign:String) {
  def getKey() = icaoId.toLowerCase
}

object AircraftID {
  def apply(icaoId:String,icaoCallsign:String):AircraftID = new AircraftID(icaoId.toLowerCase,icaoCallsign)
  def apply(icaoId:String):AircraftID = new AircraftID(icaoId.toLowerCase,"")
  def apply():AircraftID = new AircraftID("","")
}
