package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._


sealed abstract class Units(val value: Int, val name: String) extends IntEnumEntry {
  override def toString = s"${this.getClass().getSimpleName}(${value},${name})"
}
object Units extends IntEnum[Units] {
  case object METERS     extends Units(value = 0, name = "meters")
  case object FEET       extends Units(value = 1, name = "feet")
  case object FPM        extends Units(value = 2, name = "fpm")
  case object MPS        extends Units(value = 3, name = "m/s")
  case object KNOTS      extends Units(value = 4, name = "kt")
  case object MPH        extends Units(value = 5, name = "mph")
  case object KPH        extends Units(value = 6, name = "km/h")
  
  val values = findValues

  def withName(name:String): Units = { 
    values.filter(_.name == name).headOption.getOrElse(METERS)
   }
}

