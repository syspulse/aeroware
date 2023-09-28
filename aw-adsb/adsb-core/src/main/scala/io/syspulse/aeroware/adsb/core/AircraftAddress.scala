package io.syspulse.aeroware.adsb.core

import enumeratum._
import enumeratum.values._
import io.syspulse.aeroware.core.AircraftID

case class AircraftAddress(icaoId:String,icaoType:String,icaoCallsign:String) {
  def getKey() = icaoId.toLowerCase
  def toId():AircraftID = AircraftID(icaoId,icaoCallsign)
}

object AircraftAddress {
  def apply(icaoId:String,icaoType:String,icaoCallsign:String):AircraftAddress = new AircraftAddress(icaoId.toLowerCase,icaoType,icaoCallsign)
  def apply(icaoId:String):AircraftAddress = new AircraftAddress(icaoId.toLowerCase,"","")
}
