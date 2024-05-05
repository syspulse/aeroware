package io.syspulse.aeroware.aircraft.server

import io.syspulse.skel.service.JsonCommon
import io.syspulse.aeroware.aircraft.Aircraft
import io.syspulse.aeroware.aircraft.store.AircraftRegistry._

import spray.json.DefaultJsonProtocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, deserializationError}

import io.syspulse.aeroware.aircraft._

object AircraftJson extends JsonCommon {
  
  import DefaultJsonProtocol._

  implicit val jf_Aircraft = jsonFormat7(Aircraft.apply _)
  implicit val jf_Aircrafts = jsonFormat2(Aircrafts)
  implicit val jf_AircraftRes = jsonFormat2(AircraftRes)
  implicit val jf_CreateReq = jsonFormat5(AircraftCreateReq)  
  //implicit val jf_UpdateReq = jsonFormat1(AircraftUpdateReq)  
}
