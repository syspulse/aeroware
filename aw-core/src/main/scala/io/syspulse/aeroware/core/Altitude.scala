package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

case class Altitude(alt:Double,units:Units,epsilon:Double=1.0) {
  if(units != METERS && units != FEET && units != KM) throw new IllegalArgumentException(s"Invalid Altitude units: ${units}")

  def meters = units match {
    case METERS => alt 
    case FEET => alt * 0.3048
    case KM => alt * 1000.0
  }
  def feet = units match {
    case FEET => alt 
    case METERS => alt * 3.28084
    case KM => alt * 3280.84
  }

  def km = units match {
    case FEET => alt * 0.0003048
    case METERS => alt / 1000.0
    case KM => alt
  }

  override def equals(that:Any) = { that match {
    case x: Altitude => {
      val a = (this.meters - x.meters).abs
      a < epsilon 
      // &&
      // (if(alt < epsilon) true // zero altitude is the same in different units
      //   else 
      //   (units == x.units)
      // )
    }
    case _ => false
  }}

  override def hashCode = 
    (math.floor(alt * (1.0/epsilon)) / (1.0/epsilon)).hashCode + 
    units.hashCode
}

object Altitude {
  val UNKNOWN = Altitude(0.0,Units.METERS)
}
