package io.syspulse.aeroware.adsb.core

import spray.json._
import DefaultJsonProtocol._ 

import pl.iterators.kebs.json.KebsSpray
import pl.iterators.kebs.json.KebsEnumFormats
import pl.iterators.kebs.json.SprayJsonValueEnum

import io.syspulse.skel.service.JsonCommon
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.core._
import io.syspulse.aeroware.json.CoreJson

trait AdsbJsonProtocol extends DefaultJsonProtocol with SprayJsonValueEnum

object AdsbJson extends AdsbJsonProtocol {  
  import CoreJson._
  
  implicit val jf_aa = jsonFormat3(AircraftAddress.apply _)
  
  implicit val jf_ap = jsonFormat5(ADSB_Unknown)

  implicit val jf_ai = jsonFormat8(ADSB_AircraftIdentification)
  implicit val jf_apb = jsonFormat9(ADSB_AirbornePositionBaro)
  implicit val jf_vel = jsonFormat8(ADSB_AirborneVelocity)
  implicit val jf_as = jsonFormat5(ADSB_AircraftStatus)

  implicit val jf_sf = jsonFormat5(ADSB_SurfacePosition)
  implicit val jf_apg = jsonFormat5(ADSB_AirbornePositionGNSS)
  implicit val jf_r = jsonFormat5(ADSB_Reserved)
  implicit val jf_ts = jsonFormat5(ADSB_TargetState)
  implicit val jf_aos = jsonFormat5(ADSB_AircraftOperationStatus)
}
