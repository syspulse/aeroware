package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

case class Altitude(alt:Double,units:Units) {
  def meters = if(units == METERS) alt else alt * 0.3048
  def feet = if(units == FEET) alt else alt / 0.3048
}

