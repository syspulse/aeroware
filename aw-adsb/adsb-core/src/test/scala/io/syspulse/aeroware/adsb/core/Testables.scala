package io.syspulse.aeroware.adsb.core

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb.util._

trait Testables {

  val file1 = "test-1.adsb"
  val flightFile1 = "flight-1.adsb"

  def load(file:String) = {
    val txt = scala.io.Source.fromResource(file).getLines()
    txt.toSeq.filter(_.trim.size>0).map( s => Dump1090.decode(s))
  }
}
