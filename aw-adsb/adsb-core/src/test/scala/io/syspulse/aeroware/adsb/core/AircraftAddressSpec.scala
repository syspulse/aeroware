package io.syspulse.aeroware.adsb.core

import scala.util._

import scodec.bits._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class AircraftAddressSpec extends WordSpec with Matchers with Testables {

  val v1 = AircraftAddress("UK-CQF-001","C-172","UR-CQF")
  val v2 = AircraftAddress("UK-CQF-001","C-172","UR-CQF")
  val v3 = AircraftAddress("4840d6","","")

  "AircraftAddressSpec" should {
    
    s"compare itselves" in {
      v1 should === (v1)
      v2 should === (v2)
      v3 should === (v3)
    }

    s"compare ${v1} == ${v2}" in {
      v1 should === (v2)
    }

    s"NOT compare ${v1} == ${v3}" in {
      v1 should !== (v3)
    }
  }  
}
