package io.syspulse.aeroware.adsb.core

import enumeratum._
import enumeratum.values._

case class AircraftAddress(icaoId:String,icaoType:String,icaoCallsign:String) {
  def getKey() = s"${icaoId}/${icaoType}/${icaoCallsign}"
}

object AircraftAddress {
  def apply(icaoId:String,icaoType:String,icaoCallsign:String):AircraftAddress = new AircraftAddress(icaoId.toLowerCase,icaoType,icaoCallsign)
}
