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
  case object GS      extends SpeedType(value = 3, name = "GS")
  
  val values = findValues

  def withName(name:String): SpeedType = { 
    values.filter(_.name == name).headOption.getOrElse(IAS)
   }
}

case class Speed(v:Double,units:Units,typ:SpeedType = SpeedType.IAS,epsilon:Double=0.1) {
  if(units != KNOTS && units != MPH && units != KPH) throw new IllegalArgumentException(s"Invalid Speed units: ${units}")

  def knots = units match {
    case KNOTS => v 
    case MPH => v * 0.868976
    case KPH => v * 0.539957
  }
  def mps = units match {
    case KNOTS => v * 1.94384
    case MPH => v 
    case KPH => v * 0.277778
  }

  def kmh = units match {
    case KNOTS => v * 1.852
    case MPH => v * 1.60934
    case KPH => v
  }

  override def equals(that:Any) = { that match {
    case x: Speed => {
      // convert everything to knots
      (this.knots - x.knots).abs < epsilon &&
      (this.typ == x.typ) // zero speed is not the same in different types (GS != IAS != TAS)
    }
    case _ => false
  }}

  override def hashCode = 
    (math.floor(v * (1.0/epsilon)) / (1.0/epsilon)).hashCode + 
    units.hashCode +
    typ.hashCode
}

