package io.syspulse.aeroware.core

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class VRateSpec extends WordSpec with Matchers {

  val v01 = VRate(0,Units.FPM)
  val v02 = VRate(0,Units.MPS)

  val v1 = VRate(500.5,Units.FPM)
  val v2 = VRate(500.6,Units.FPM)
  val v3 = VRate(500.5,Units.MPS)
  
  // all the same, precision is 1.0 m/s
  val v4 = VRate(100.32,Units.FPM)
  val v5 = VRate(100.31,Units.FPM)
  val v6 = VRate(101,Units.FPM)

  val v7 = VRate(450.0,Units.FPM)
  val v8 = VRate(2.286,Units.MPS)
  
  "VRateSpec" should {
    
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
      
    }

    s"expect Exception for invalid Units" in {
      assertThrows[IllegalArgumentException] { 
        VRate(0,Units.METERS)
      }

      assertThrows[IllegalArgumentException] { 
        VRate(0,Units.KNOTS)
      }
    }

    s"compare ${v01} == ${v01}" in {
      v01 should === (v01)
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

    s"compare same VRate in FPM vs MPS" in {
      v7 should === (v8)
    }
    
  }  
}
