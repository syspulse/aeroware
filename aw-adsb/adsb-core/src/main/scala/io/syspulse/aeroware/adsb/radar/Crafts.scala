package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import scala.collection.mutable

import com.typesafe.scalalogging.Logger
import io.syspulse.aeroware.core.AircraftID

abstract class Craft(aid:AircraftID) extends Trackable(aid) {
}

case class Aircraft(aid:AircraftID) extends Craft(aid)
case class Helicopter(aid:AircraftID) extends Craft(aid)
case class Firetruck(aid:AircraftID) extends Craft(aid)
case class Glider(aid:AircraftID) extends Craft(aid)
case class Drone(aid:AircraftID) extends Craft(aid)
case class Vehicle(aid:AircraftID) extends Craft(aid)

object Craft {
  def apply(aid:AircraftID):Craft = {
    new Aircraft(aid) 
  }
}

