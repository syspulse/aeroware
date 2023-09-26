package io.syspulse.aeroware.adsb.core

import enumeratum._
import enumeratum.values._

case class AircraftAddress(icaoId:String,icaoType:String,icaoCallsign:String) {
  def getKey() = icaoId.toLowerCase
}

object AircraftAddress {
  def apply(icaoId:String,icaoType:String,icaoCallsign:String):AircraftAddress = new AircraftAddress(icaoId.toLowerCase,icaoType,icaoCallsign)
  def apply(icaoId:String):AircraftAddress = new AircraftAddress(icaoId.toLowerCase,"","")
}
