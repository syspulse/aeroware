package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

sealed abstract class SpeedType(val value: Int, val name: String) extends IntEnumEntry {
  override def toString = s"${this.getClass().getSimpleName}(${value},${name})"
}
object SpeedType extends IntEnum[SpeedType] {
  case object TAS     extends SpeedType(value = 0, name = "TAS")
  case object CAS     extends SpeedType(value = 1, name = "CAS")
  case object IAS     extends SpeedType(value = 2, name = "IAS")
  
  val values = findValues

  def withName(name:String): SpeedType = { 
    values.filter(_.name == name).headOption.getOrElse(IAS)
   }
}

case class Speed(v:Double,units:Units,typ:SpeedType = SpeedType.IAS) {
  def knots = units match {
    case KNOTS => v 
    case MPH => v * 1.94384
    case KPH => v * 0.539957
  }
  def mps = units match {
    case KNOTS => v * 0.868976
    case MPH => v 
    case KPH => v * 0.277778
  }

  def kmh = units match {
    case KNOTS => v * 1.852
    case MPH => v * 1.60934
    case KPH => v
  }
}

