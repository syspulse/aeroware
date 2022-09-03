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


trait UnitsJsonProtocol extends DefaultJsonProtocol with SprayJsonValueEnum { 
  // implicit object UnitsJsonFormat extends RootJsonFormat[Units] {
  //   def write(t: Units) = {
  //     JsObject(
  //       //"value" -> JsNumber(t.value),
  //       "name" -> JsString(t.name),
  //     )
  //   }

  //   def read(value: JsValue):Units = value match {
  //     case JsArray(
  //       Vector(
  //         JsString(name), 
  //         //JsNumber(value)
  //       )) => Units.withName(name)
  //     case _ => deserializationError("name expected")
  //   }
  // }
}

object AdsbJson extends UnitsJsonProtocol {
  //import DefaultJsonProtocol._
  //import UnitsJsonProtocol._
 
  implicit val jf_aa = jsonFormat3(AircraftAddress.apply _)
  implicit val jf_un = jsonFormat(Units)

  implicit val jf_al = jsonFormat3(Altitude.apply _)
  implicit val jf_l = jsonFormat4(Location.apply _)
  
  implicit val jf_ap = jsonFormat5(ADSB_Unknown)

  implicit val jf_ai = jsonFormat8(ADSB_AircraftIdentification)
  implicit val jf_apb = jsonFormat9(ADSB_AirbornePositionBaro)
}

object AdsbIngestedJsonProtocol extends DefaultJsonProtocol with UnitsJsonProtocol { 
  
  implicit object AdsbIngestedJsonFormat extends JsonFormat[ADSB_Ingested] {
    import AdsbJson._
        
    def write(a: ADSB_Ingested):JsValue = {      
      a.adsb match {
        case adsb:ADSB_AircraftIdentification => adsb.toJson
        case adsb:ADSB_AirbornePositionBaro => adsb.toJson
        //case adsb:ADSB_AirborneVelocity => 
        case _ => JsString(a.adsb.toString)
      }      
    }

    def read(value: JsValue):ADSB_Ingested = value match {
      case _ => deserializationError("not supported")
    }
  }
}
