package io.syspulse.aeroware.metar

import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Locale
import io.jvm.uuid._

import fastparse._, NoWhitespace._
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import scala.util.{Try,Success,Failure}


abstract class Altitude {

}

case class Alt(alt:Long,altTil:Long = -1L) extends Altitude
case class FL(fl:Int, flTil:Int = -1) extends Altitude

object Altitude {
  
  def ws[_: P] = P( " ".rep(1) )

  def altFLParser[_: P] = P("FL" ~ CharIn("0-9").rep().!).map(p => FL(p.toInt))
  def altFeetParser[_: P] = P(CharIn("0-9").rep(1).!).map(p => Alt(p.toLong))
  def altMeterParser[_: P] = P(CharIn("0-9").rep(1).! ~ ws ~ "M").map(p => Alt((p.toDouble * 3.28084).toLong))

  def altFLRangeParser[_: P] = P("FL" ~ CharIn("0-9").rep().! ~ "-" ~ CharIn("0-9").rep().!).map(p => FL(p._1.toInt,p._2.toInt))
  
  def altParser[_: P] = P( altFLRangeParser | altFLParser | altFeetParser | altMeterParser )

  def decode(data:String): Try[Altitude] = {
    val alt = parse(data.trim, altParser(_))
    alt.fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(h))
  }
}


