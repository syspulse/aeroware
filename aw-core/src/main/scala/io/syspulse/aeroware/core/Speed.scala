package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

case class Speed(v:Double,units:Units) {
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

