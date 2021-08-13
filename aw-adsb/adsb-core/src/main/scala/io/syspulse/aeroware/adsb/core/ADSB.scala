package io.syspulse.aeroware.asdb.core

import io.syspulse.aeroware.asdb.core.Decoder

package object adsb {
  type Raw = String
}

import adsb._

case class AircraftAddress(icaoId:String,icaoType:String,icaoCallsign:String)
case class AirbornePosition(raw:RawAirbornePosition)

abstract class ADSB {    
    val df:Byte
    val capability:Byte
    val aircraftAddr:AircraftAddress
    val raw:Raw
}

case class ADSB_Unknown(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB

case class ADSB_AircraftIdentification(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB

case class ADSB_SurfacePosition(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB
case class ADSB_AirbornePositionBaro(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB
case class ADSB_AirborneVelocity(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB
case class ADSB_AirbornePositionGNSS(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB

case class ADSB_Reserved(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB

case class ADSB_AircraftStatus(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB
case class ADSB_TargetState(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB
case class ADSB_AircraftOperationStatus(df:Byte,capability:Byte, aircraftAddr:AircraftAddress, raw:Raw) extends ADSB

