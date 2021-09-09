package io.syspulse.aeroware.adsb.live

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import scala.collection._

class Aircrafts { 
  val aircrafts:mutable.Map[AircraftAddress,Aircraft] = mutable.HashMap()

  def +(a:Aircraft):Aircraft = { aircrafts.put(a.getId,a); a}
  def find(id:AircraftAddress):Option[Aircraft] = aircrafts.get(id)
}


