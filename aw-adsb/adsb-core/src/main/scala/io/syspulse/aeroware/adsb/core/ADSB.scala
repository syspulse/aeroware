package io.syspulse.aeroware.adsb.core

import io.syspulse.aeroware.adsb.core.Decoder

import enumeratum._
import enumeratum.values._

import io.syspulse.aeroware.core.{ Units, Altitude, Location}

package object adsb {
  type Raw = String
}

import adsb._
import ADSB._


case class AircraftAddress(icaoId:String,icaoType:String,icaoCallsign:String)
case class AirbornePosition(raw:RawAirbornePosition)

abstract class ADSB {
    val ts:Long // timestamp
    val df:Byte
    val capability:Byte
    val aircraftAddr:AircraftAddress
    val raw:Raw
}

case class ADSB_Unknown(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw, ts:Long=now) extends ADSB

case class ADSB_AircraftIdentification(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, tc:Byte, ec:Byte, callSign:String, raw:Raw, ts:Long=now) extends ADSB

case class ADSB_SurfacePosition(df:Byte,capability:Byte, aircraftAddr:AircraftAddress,raw:Raw,ts:Long=now) extends ADSB
case class ADSB_AirbornePositionBaro(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, loc:Location, isOdd:Boolean,latCPR:Double,lonCPR:Double,raw:Raw,ts:Long=now) extends ADSB {
  
  def getLocalPosition(ref:ADSB_AirbornePositionBaro): ADSB_AirbornePositionBaro = {
    ADSB_AirbornePositionBaro(
      df, capability, aircraftAddr,
      loc = Decoder.getLocalPosition(ref.loc, isOdd, latCPR, lonCPR, loc.alt),
      isOdd = isOdd, latCPR = latCPR, lonCPR = lonCPR, raw = raw, ts = ts
    )
  }
}

case class ADSB_AirborneVelocity(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, hSpeed:Double, heading:Double, vRate:Double, raw:Raw,ts:Long=now) extends ADSB
case class ADSB_AirbornePositionGNSS(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB

case class ADSB_Reserved(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB

case class ADSB_AircraftStatus(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB
case class ADSB_TargetState(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB
case class ADSB_AircraftOperationStatus(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB

object AircraftAddress {
  def apply(icaoId:String,icaoType:String,icaoCallsign:String):AircraftAddress = new AircraftAddress(icaoId.toLowerCase,icaoType,icaoCallsign)
}

object ADSB {
  def now = System.currentTimeMillis()
}