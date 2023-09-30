package io.syspulse.aeroware.json

import spray.json._
import DefaultJsonProtocol._ 

import pl.iterators.kebs.json.KebsSpray
import pl.iterators.kebs.json.KebsEnumFormats
import pl.iterators.kebs.json.SprayJsonValueEnum

import io.syspulse.skel.service.JsonCommon
import io.syspulse.aeroware.core._

object CoreJson extends JsonCommon with SprayJsonValueEnum {
  //import DefaultJsonProtocol._

  implicit val jf_aid = jsonFormat2(AircraftID.apply _)  
  implicit val jf_un = jsonFormat(Units)
  implicit val jf_st = jsonFormat(SpeedType)

  implicit val jf_al = jsonFormat3(Altitude.apply _)
  implicit val jf_l = jsonFormat4(Location.apply _)
  implicit val jf_vr = jsonFormat3(VRate.apply _)
  implicit val jf_sp = jsonFormat4(Speed.apply _)
  
}
