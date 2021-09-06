package io.syspulse.aeroware.adsb.core

import scala.util._

import scodec.bits._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.core.Units
import io.syspulse.aeroware.core.Altitude
import io.syspulse.aeroware.core.Location
import org.scalactic.TolerantNumerics

class AirborneVelocitySpec extends WordSpec with Matchers with Testables {

  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.01)
  
  val msg1 = "8D485020994409940838175B284F" 
  
  val msg3 = "8DA05F219B06B6AF189400CBC33F" 
  
  "AirborneVelocitySpec" should {

    s"decode ${msg1} as ADSB_AirborneVelocity type Subtype-1" in {
      val a1 = Decoder.decode(msg1)
      a1.get.getClass should === (ADSB_AirborneVelocity(17,19,AircraftAddress("485020","",""),0,0,0,raw=null).getClass)
    }

    s"decode ${msg1} ADSB_AirborneVelocity with hSpeed 159.20 kt" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.hSpeed should === (159.2)
    }

    s"decode ${msg1} ADSB_AirborneVelocity with header 182.88 deg" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.heading should === (182.88)
    }

    s"decode ${msg1} ADSB_AirborneVelocity with vRate 832 fpm" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.vRate should === (-832.0)
    }
  
    s"decode ${msg3} as ADSB_AirborneVelocity type Subtype-3" in {
      val a1 = Decoder.decode(msg3)
      a1.get.getClass should === (ADSB_AirborneVelocity(17,19,AircraftAddress("A05F21","",""),0,0,0,raw=null).getClass)
    }

    s"decode ${msg3} ADSB_AirborneVelocity with hSpeed 376 kt" in {
      val a1 = Decoder.decode(msg3).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.hSpeed should === (376.0)
    }

    s"decode ${msg3} ADSB_AirborneVelocity with header 243.98 deg" in {
      val a1 = Decoder.decode(msg3).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.heading should === (243.98)
    }

    s"decode ${msg3} ADSB_AirborneVelocity with vRate 832 fpm" in {
      val a1 = Decoder.decode(msg3).get.asInstanceOf[ADSB_AirborneVelocity]
      a1.vRate should === (-2304.0)
    }
  }
}
