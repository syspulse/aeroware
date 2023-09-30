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

        case adsb:ADSB_Unknown => adsb.toJson

        case _ => JsString(a.adsb.toString)
      }      
    }

    def read(value: JsValue):ADSB_Ingested = value match {
      case _ => deserializationError("not supported")
    }
  }
}
