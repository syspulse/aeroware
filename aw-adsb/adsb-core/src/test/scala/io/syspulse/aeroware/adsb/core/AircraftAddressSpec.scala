package io.syspulse.aeroware.adsb.core

import scala.util._

import scodec.bits._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class AircraftAddressSpec extends WordSpec with Matchers with Testables {

  val a3 = AircraftAddress("4840d6","","")
  val a1 = AircraftAddress("UK-CQF-001","C-172","UR-CQF")
  val a2 = AircraftAddress("UK-CQF-001","C-172","UR-CQF")

  "AircraftAddressSpec" should {
    
    s"compare ${a1} == ${a2}" in {
      a1 should === (a2)
    }

    s"NOT compare ${a1} == ${a3}" in {
      a1 should !== (a3)
    }
  }  
}
