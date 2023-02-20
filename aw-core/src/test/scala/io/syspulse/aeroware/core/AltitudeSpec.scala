package io.syspulse.aeroware.core

import scala.util._

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class AltitudeSpec extends AnyWordSpec with Matchers {

  val v01 = Altitude(0,Units.METERS)
  val v02 = Altitude(0,Units.FEET)

  val v1 = Altitude(1000.5,Units.METERS)
  val v2 = Altitude(1000.6,Units.METERS)
  val v3 = Altitude(1000.5,Units.FEET)
  
  // all the same, precision is 0.1 meter
  val v4 = Altitude(2500,Units.FEET)
  val v5 = Altitude(2500.31,Units.FEET)
  val v6 = Altitude(2501,Units.FEET)

  val v7 = Altitude(2550.0,Units.FEET)
  val v8 = Altitude(777.24,Units.METERS)
  val v9 = Altitude(0.77724,Units.KM)

  "AltitudeSpec" should {
    
    s"compare selves" in {
      v01 should === (v01)
      v02 should === (v02)
      v1 should === (v1)
      v2 should === (v2)
      v3 should === (v3)
      v4 should === (v4)
      v5 should === (v5)
      v6 should === (v6)
      v7 should === (v7)
      v8 should === (v8)
      v9 should === (v9)
      
    }

    s"expect Exception for invalid Units" in {
      assertThrows[IllegalArgumentException] { 
        Altitude(0,Units.KNOTS)
      }

      assertThrows[IllegalArgumentException] { 
        Altitude(0,Units.MPS)
      }

      assertThrows[IllegalArgumentException] { 
        Altitude(0,Units.KPH)
      }
    }

    s"compare ${v01} == ${v02}" in {
      v01 should === (v02)
    }

    s"compare ${v1} == ${v2}" in {
      v1 should === (v2)
    }

    s"NOT compare ${v1} == ${v3}" in {
      v1 should !== (v3)
    }

    s"compare ${v4} == ${v5}" in {
      v4 should === (v5)
    }

    s"compare ${v4} == ${v6}" in {
      v4 should === (v6)
    }
    
    s"compare same Alt in FEET vs METERS" in {
      v7 should === (v8)
    }

    s"compare same Alt in FEET vs KM" in {
      v7 should === (v9)
    }

    s"compare same Alt in KM vs METERS" in {
      v8 should === (v9)
    }
  }  
}
