package io.syspulse.aeroware.adsb.core

import io.syspulse.aeroware.adsb.core.Decoder

import enumeratum._
import enumeratum.values._

import io.syspulse.aeroware.core.{ Units, Altitude, Location, Speed, VRate}
import io.syspulse.skel.util.Util

package object adsb {
  type Raw = String
}

import adsb._
import ADSB._

case class AirbornePosition(raw:RawAirbornePosition)

abstract class ADSB extends Serializable {
    val ts:Long // timestamp
    val df:Byte
    val capability:Byte
    val addr:AircraftAddress
    val raw:Raw
} 

// Undecoded and passed down the pipeline for investigation
case class ADSB_Failure(err:String, raw:Raw, df:Byte = 0,capability:Byte = 0, addr:AircraftAddress = AircraftAddress("","",""), ts:Long=now) extends ADSB

case class ADSB_Unknown(df:Byte,capability:Byte, addr:AircraftAddress, raw:Raw, ts:Long=now) extends ADSB

case class ADSB_AircraftIdentification(df:Byte,capability:Byte, addr:AircraftAddress, tc:Byte, ec:Byte, callSign:String, raw:Raw, ts:Long=now) extends ADSB

case class ADSB_SurfacePosition(df:Byte,capability:Byte, addr:AircraftAddress,raw:Raw,ts:Long=now) extends ADSB
case class ADSB_AirbornePositionBaro(df:Byte,capability:Byte, addr:AircraftAddress, loc:Location, isOdd:Boolean,latCPR:Double,lonCPR:Double,raw:Raw,ts:Long=now) extends ADSB {
  
  def getLocalPosition(ref:ADSB_AirbornePositionBaro): ADSB_AirbornePositionBaro = {
    ADSB_AirbornePositionBaro(
      df, capability, addr,
      loc = Decoder.getLocalPosition(ref.loc, isOdd, latCPR, lonCPR, loc.alt),
      isOdd = isOdd, latCPR = latCPR, lonCPR = lonCPR, raw = raw, ts = ts
    )
  }
}

case class ADSB_AirborneVelocity(df:Byte,capability:Byte, addr:AircraftAddress, hSpeed:Speed, heading:Double, vRate:VRate, raw:Raw,ts:Long=now) extends ADSB

case class ADSB_AirbornePositionGNSS(df:Byte,capability:Byte, addr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB

case class ADSB_Reserved(df:Byte,capability:Byte, addr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB

case class ADSB_AircraftStatus(df:Byte,capability:Byte, addr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB
case class ADSB_TargetState(df:Byte,capability:Byte, addr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB
case class ADSB_AircraftOperationStatus(df:Byte,capability:Byte, addr:AircraftAddress, raw:Raw,ts:Long=now) extends ADSB

case class ADSB_AllCall(df:Byte,capability:Byte,addr:AircraftAddress, parity:Array[Byte], raw:Raw, ts:Long=now) extends ADSB {
  def isAirborne() = capability == 5
  def isGround() = capability == 4

  val interrogator = {
    Decoder.decodeParity(parity).zip(parity).map{ case(a1,a2) => (a1 ^ a2).toByte }    
  }

  val code = {
		((interrogator(2) >> 4) & 0x7).toByte match {
			case 0x0 =>
			case 0x1 => (interrogator(2) &0x0f).toByte
			case 0x2 => ((interrogator(2) &0x0f) + 16).toByte
			case 0x3 => ((interrogator(2) &0x0f) + 32).toByte
			case _ => ((interrogator(2) & 0x0f) + 48).toByte
		}
	}

  override def toString = s"ADSB_AllCall(${df},${capability},${addr},${Util.hex(parity)},${Util.hex(interrogator)},${code},${raw},${ts})"
}

object ADSB {
  def now = System.currentTimeMillis()
}