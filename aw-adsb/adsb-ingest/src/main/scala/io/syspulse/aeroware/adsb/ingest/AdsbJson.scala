package io.syspulse.aeroware.adsb.ingest

import io.syspulse.skel.service.JsonCommon


import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.core._

import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, deserializationError}
import spray.json.RootJsonFormat
import spray.json.JsObject
import spray.json.JsNumber
import spray.json.JsArray
import spray.json._
import DefaultJsonProtocol._ 

import pl.iterators.kebs.json.KebsSpray
import pl.iterators.kebs.json.KebsEnumFormats
import pl.iterators.kebs.json.SprayJsonValueEnum


trait AdsbJsonProtocol extends DefaultJsonProtocol with SprayJsonValueEnum

object AdsbJson extends AdsbJsonProtocol {
  //import DefaultJsonProtocol._
  
  implicit val jf_aa = jsonFormat3(AircraftAddress.apply _)
  implicit val jf_un = jsonFormat(Units)
  implicit val jf_st = jsonFormat(SpeedType)

  implicit val jf_al = jsonFormat3(Altitude.apply _)
  implicit val jf_l = jsonFormat4(Location.apply _)
  implicit val jf_vr = jsonFormat3(VRate.apply _)
  implicit val jf_sp = jsonFormat4(Speed.apply _)
  
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

object AdsbIngestedJsonProtocol extends DefaultJsonProtocol with AdsbJsonProtocol { 
  
  implicit object AdsbIngestedJsonFormat extends JsonFormat[ADSB_Ingested] {
    import AdsbJson._
        
    def write(a: ADSB_Ingested):JsValue = {      
      a.adsb match {
        case adsb:ADSB_AircraftIdentification => adsb.toJson
        case adsb:ADSB_AirbornePositionBaro => adsb.toJson
        case adsb:ADSB_AirborneVelocity => adsb.toJson
        case adsb:ADSB_AircraftStatus => adsb.toJson
        
        case adsb:ADSB_SurfacePosition => adsb.toJson
        case adsb:ADSB_AirbornePositionGNSS => adsb.toJson
        case adsb:ADSB_Reserved => adsb.toJson
        case adsb:ADSB_TargetState => adsb.toJson
        case adsb:ADSB_AircraftOperationStatus => adsb.toJson

        case _ => JsString(a.adsb.toString)
      }      
    }

    def read(value: JsValue):ADSB_Ingested = value match {
      case _ => deserializationError("not supported")
    }
  }
}
