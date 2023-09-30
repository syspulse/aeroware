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
import org.scalactic.TolerantNumerics

class AirbornePositionSpec extends AnyWordSpec with Matchers with Testables {

  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.0001)
  
  val msg1 = "8D40621D58C382D690C8AC2863A7" // latest (newest)
  val msg2 = "8D40621D58C386435CC412692AD6" // previous (oldest)

  val flight1 = Seq(
"8D5080355811B53B1416D87A32C7",
"8D5080355811C53AFA169AA6CD17",
"8D5080355811D53ABE160FD3E286",
"8D5080355811D53ABA16061B37E7",
"8D5080355811D53AB615FDAAEEDD",
"8D5080355811D1CADA412CAE5755",
"8D5080355811D53A7A156E6BEF24",
"8D5080355813053A0C147AC637E3",
"8D5080355811D53984133F75CD28",
"8D5080355811B1C95A3DAC76F696",
"8D5080355811B5392E1271018FC7",
"8D508035600BB5331234769746E9"
  )

  "AirbornePositionSpec" should {

    s"decode ${msg1} as ADSB_AirbornePositionBaro type" in {
      val a1 = Adsb.decode(msg1)
      a1.get.getClass should === (ADSB_AirbornePositionBaro(17,5,AircraftAddress("40621D","",""),loc=null,isOdd=false, latCPR=0, lonCPR=0, raw=null).getClass)
    }

    s"decode ${msg1} ADSB_AirbornePositionBaro with altitude 38000 feet" in {
      val a1 = Adsb.decode(msg1).get.asInstanceOf[ADSB_AirbornePositionBaro]
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

    s"decode Location from ${msg1},${msg2}" in {
      val decoder = new Decoder()
      
      val a1 = decoder.decode(msg2).get.asInstanceOf[ADSB_AirbornePositionBaro]
      val a2 = decoder.decode(msg1).get.asInstanceOf[ADSB_AirbornePositionBaro]

      val loc = Adsb.getGloballPosition(a1,a2)
      //info(loc.toString)
    }

    // s"decode Flight from ${flight1}" in {
    //   val decoder = new Decoder()
      
    //   var a0 = decoder.decode(flight1.head).get.asInstanceOf[ADSB_AirbornePositionBaro]

    //   for( m <- flight1.tail ) {
    //     val a1 = decoder.decode(m).get.asInstanceOf[ADSB_AirbornePositionBaro]
    //     val loc = Decoder.getGloballPosition(a0,a1)
    //     info(loc.toString)
    //     a0 = a1
    //   }
    // }

    s"decode Flight from file=${flightFile1}" in {
      val decoder = new Decoder()
      val flight = load(flightFile1)
      
      var a0 = decoder.decode(flight.head).get.asInstanceOf[ADSB_AirbornePositionBaro]

      for( m <- flight.tail ) {
        val a1 = decoder.decode(m).get.asInstanceOf[ADSB_AirbornePositionBaro]
        val loc = Adsb.getGloballPosition(a0,a1)
        //println(s"${loc.lat},${loc.lon},${loc.alt.alt}")
        a0 = a1
      }
    }
    
  }  
}
