package io.syspulse.aeroware.adsb.core

import scala.util._

import scodec.bits._

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb.Testables
import io.syspulse.aeroware.core.Units
import io.syspulse.aeroware.core.Altitude
import io.syspulse.aeroware.core.Location
import io.syspulse.aeroware.core.VRate
import io.syspulse.aeroware.core.Speed

import org.scalactic.Equality
import org.scalactic.TolerantNumerics

class AirborneVelocitySpec extends AnyWordSpec with Matchers with Testables {

  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.01)
  implicit val speedEq = new Equality[Speed] { def areEqual(a: Speed, b: Any): Boolean =
            b match {
              case p: Speed => a.units == p.units && a.v === p.v +- 0.01
              case _ => false
            }}
  implicit val vRateEq = new Equality[VRate] { def areEqual(a: VRate, b: Any): Boolean =
            b match {
            case p: VRate => a.units == p.units && a.v === p.v +- 0.01
              case _ => false
            }}
  
  val msg1 = "8D485020994409940838175B284F" 
  
  val msg3 = "8DA05F219B06B6AF189400CBC33F" 
  
  "AirborneVelocitySpec" should {

    s"decode ${msg1} as ADSB_AirborneVelocity type Subtype-1" in {
      val a1 = Decoder.decode(msg1)
      a1.get.getClass should === (ADSB_AirborneVelocity(17,19,AircraftAddress("485020","",""),Speed(0,Units.KNOTS),0,VRate(0,Units.FPM),raw=null).getClass)
    }

    s"decode ${msg1} ADSB_AirborneVelocity with hSpeed 159.20 kt" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.hSpeed should === (Speed(159.2,Units.KNOTS))
    }

    s"decode ${msg1} ADSB_AirborneVelocity with heading 182.88 deg" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.heading should === (183.0)
    }

    s"decode ${msg1} ADSB_AirborneVelocity with vRate 832 fpm" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.vRate should === (VRate(-832.0,Units.FPM))
    }
  
    s"decode ${msg3} as ADSB_AirborneVelocity type Subtype-3" in {
      val a1 = Decoder.decode(msg3)
      a1.get.getClass should === (ADSB_AirborneVelocity(17,19,AircraftAddress("A05F21","",""),Speed(0,Units.KNOTS),0,VRate(0,Units.FPM),raw=null).getClass)
    }

    s"decode ${msg3} ADSB_AirborneVelocity with hSpeed 376 kt" in {
      val a1 = Decoder.decode(msg3).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.hSpeed should === (Speed(376.0,Units.KNOTS))
    }

    s"decode ${msg3} ADSB_AirborneVelocity with header 243.98 deg" in {
      val a1 = Decoder.decode(msg3).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.heading should === (243.98)
    }

    s"decode ${msg3} ADSB_AirborneVelocity with vRate 832 fpm" in {
      val a1 = Decoder.decode(msg3).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.vRate should === (VRate(-2304.0,Units.FPM))
    }
  }
}
