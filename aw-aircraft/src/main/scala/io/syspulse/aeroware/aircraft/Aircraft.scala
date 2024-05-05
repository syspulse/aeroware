package io.syspulse.aeroware.aircraft

import scala.jdk.CollectionConverters._
import scala.collection.immutable
import io.jvm.uuid._

case class Aircraft(  
  id:Aircraft.ID,                          // icao id (addr)  
  rid:String,                              // registration id
  model:String = "",                       // model 
  typ:String = "",                         // type
  op:Option[String] = None,                // operator  
  call:Option[String] = None,
  ts:Long = System.currentTimeMillis,      // last timestamp of aircraft registration
)

final case class Aircrafts(data: Seq[Aircraft],total:Option[Long]=None)

final case class AircraftCreateReq(
  id:Aircraft.ID, 
  rid:String,
  model:String,
  typ:String,
  call:Option[String], 
)

final case class AircraftRes(status: String,id:Option[String])


object Aircraft {
  type ID = String
  def uid(a:Aircraft):ID = s"${a.id}"
}