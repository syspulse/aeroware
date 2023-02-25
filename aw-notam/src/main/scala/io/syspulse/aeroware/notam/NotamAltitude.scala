package io.syspulse.aeroware.notam

import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Locale
import io.jvm.uuid._

import fastparse._, NoWhitespace._
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import scala.util.{Try,Success,Failure}

import io.syspulse.aeroware.core.Altitude
import io.syspulse.aeroware.core.Units

object NotamAltitude {
  
  def ws[_: P] = P( " ".rep(1) )

  def altSurface[_: P] = P("SFC").!.map(_ => Altitude(0,Units.FEET))
  def altFLParser[_: P] = P("FL" ~ CharIn("0-9").rep().!).map(p => Altitude(p.toInt * 100,Units.FEET))
  def altFeetParser[_: P] = P(CharIn("0-9").rep(1).! ~ "FT" ~ (ws ~ "AMSL").?).map(p => Altitude(p.toDouble,Units.FEET))
  def altMeterParser[_: P] = P(CharIn("0-9").rep(1).! ~ ("M" | (ws ~ "M")).?).map(p => Altitude(p.toDouble,Units.METERS))

  def altParser[_: P] = P( altSurface | altFLParser | altFeetParser | altMeterParser )
}


