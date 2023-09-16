package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import scala.collection.mutable

import com.typesafe.scalalogging.Logger
import io.syspulse.aeroware.adsb.core.AircraftAddress

abstract class Craft(addr:AircraftAddress) extends Trackable(addr) {
  def getAddr() = addr
}

case class Aircraft(addr:AircraftAddress) extends Craft(addr)
case class Helicopter(addr:AircraftAddress) extends Craft(addr)
case class Firetruck(addr:AircraftAddress) extends Craft(addr)
case class Glider(addr:AircraftAddress) extends Craft(addr)
case class Drone(addr:AircraftAddress) extends Craft(addr)
case class Vehicle(addr:AircraftAddress) extends Craft(addr)

object Craft {
  def apply(addr:AircraftAddress):Craft = {
    new Aircraft(addr) 
  }
}

