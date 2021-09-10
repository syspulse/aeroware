package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

case class VRate(v:Double,units:Units,epsilon:Double=1.0) {
  if(units != FPM && units != MPS) throw new IllegalArgumentException(s"Invalid VRate units: ${units}")

  def fpm = units match {
    case FPM => v 
    case MPS => v * 196.85
  }

  def mps = units match {
    case FPM => v * 0.00508
    case MPS => v
  }

  override def equals(that:Any) = { that match {
    case x: VRate => {
      // convert everything to m/s
      (this.mps - x.mps).abs < epsilon
    }
    case _ => false
  }}

  override def hashCode = 
    (math.floor(v * (1.0/epsilon)) / (1.0/epsilon)).hashCode + 
    units.hashCode
}

object VRate {
  val UNKNOWN = VRate(0.0,Units.MPS)
}
