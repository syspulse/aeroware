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

class AirbornePositionSpec extends WordSpec with Matchers with Testables {

  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.0001)
  
  val msg1 = "8D40621D58C382D690C8AC2863A7"
  val msg2 = "8D40621D58C386435CC412692AD6"

  "AirbornePositionSpec" should {

    s"decode ${msg1} as ADSB_AirbornePositionBaro type" in {
      val a1 = Decoder.decode(msg1)
      a1.get.getClass should === (ADSB_AirbornePositionBaro(17,5,AircraftAddress("40621D","",""),loc=null,isOdd=false, latCPR=0, lonCPR=0, raw=null).getClass)
    }

    s"decode ${msg1} ADSB_AirbornePositionBaro with altitude 38000 feet" in {
      val a1 = Decoder.decode(msg1).get.asInstanceOf[ADSB_AirbornePositionBaro]
      a1.loc.alt should === (Altitude(38000,Units.FEET))
    }

    s"decode ${msg1} ADSB_AirbornePositionBaro with Lat: 52.25720 (North)" in {
      val a1 = new Decoder(decoderLocation = Location(52.258,3.918)).decode(msg1).get.asInstanceOf[ADSB_AirbornePositionBaro]
      a1.loc.lat should === (52.25720)
    }

    s"decode ${msg1} ADSB_AirbornePositionBaro with Lon: 3.91937 (E)" in {
      val a1 = new Decoder(decoderLocation = Location(52.258,3.918)).decode(msg1).get.asInstanceOf[ADSB_AirbornePositionBaro]
      a1.loc.lon should === (3.91937)
    }
    
  }  
}
