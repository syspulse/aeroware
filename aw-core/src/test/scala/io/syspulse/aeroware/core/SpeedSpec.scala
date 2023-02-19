package io.syspulse.aeroware.core

import scala.util._

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class SpeedSpec extends AnyWordSpec with Matchers {

  val v01 = Speed(0,Units.KNOTS)
  val v02 = Speed(0,Units.KPH)

  val v1 = Speed(120.5,Units.KNOTS)
  val v2 = Speed(120.6,Units.KNOTS)
  val v3 = Speed(120.5,Units.MPH)
  
  val v4 = Speed(25.32,Units.KNOTS)
  val v5 = Speed(25.31,Units.KNOTS)
  val v6 = Speed(26,Units.KNOTS)

  val v7 = Speed(25.0,Units.KNOTS)
  val v8 = Speed(28.7695,Units.MPH)
  val v9 = Speed(46.3,Units.KPH)

  val v10 = Speed(120.0,Units.KNOTS,SpeedType.IAS)
  val v11 = Speed(120.0,Units.KNOTS,SpeedType.TAS)

  "SpeedSpec" should {
    
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
      v10 should === (v10)
      v11 should === (v11)
    }

    s"expect Exception for invalid Units" in {
      assertThrows[IllegalArgumentException] { 
        Speed(0,Units.METERS)
      }

      assertThrows[IllegalArgumentException] { 
        Speed(0,Units.FPM)
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

    s"NOT compare ${v4} == ${v6}" in {
      v4 should !== (v6)
    }

    s"compare same speed in KNOTS vs MPH" in {
      v7 should === (v8)
    }

    s"compare same speed in KNOTS vs KPH" in {
      v7 should === (v9)
    }

    s"compare same speed in MPH vs KPH" in {
      v8 should === (v9)
    }

    s"NOT compare IAS anv TAS" in {
      v10 should !== (v11)
    }
    
  }  
}
