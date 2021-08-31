package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

package object core {
  type UID = Int
}

sealed abstract class Units(val value: core.UID, val name: String) extends IntEnumEntry {
  override def toString = s"${this.getClass().getSimpleName}(${value},${name})"
}
object Units extends IntEnum[Units] {
  case object METERS     extends Units(value = 0, name = "meters")
  case object FEET       extends Units(value = 1, name = "feet")
  
  val values = findValues

  def withName(name:String): Units = { 
    values.filter(_.name == name).headOption.getOrElse(METERS)
   }
}

