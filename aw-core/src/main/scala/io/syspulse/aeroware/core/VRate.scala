package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

case class VRate(v:Double,units:Units) {
  def fpm = units match {
    case FPM => v 
    case MPS => v * 196.85
  }

  def mps = units match {
    case FPM => v * 0.00508
    case MPS => v
  }
}

