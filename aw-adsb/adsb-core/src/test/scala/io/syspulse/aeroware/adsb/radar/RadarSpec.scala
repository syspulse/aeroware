package io.syspulse.aeroware.adsb.radar

import scala.util._

import scodec.bits._

import org.scalatest.{ Matchers, WordSpec }
import scala.concurrent.duration.Duration

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb.Testables
import io.syspulse.aeroware.core.Units
import io.syspulse.aeroware.core.{Altitude,VRate,Speed}
import io.syspulse.aeroware.core.Location
import io.syspulse.aeroware.adsb.core._

import org.scalactic.TolerantNumerics

class RadarSpec extends WordSpec with Matchers with Testables {

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

  val flight2 = Seq(
  "8D5080355813053A0C147AC637E3",
  "8D5080359904E486000C097CFCDB",
  "8D5080359904E0860834084F71EB",
  "8D5080355811D53984133F75CD28",
  "8D5080359904DF85C814082D59D2",
  "8D5080355811B1C95A3DAC76F696",
  "8D5080355811B5392E1271018FC7",
  "8D5080359904D985A84807C34957",
  "8D508035600BB5331234769746E9",
  )

  val decoder = new Decoder()

  "Radar" should {

    s"get 1 event and have size == 1 Aircraft" in {
      val radar = new Radar
      val a1 = decoder.decode(msg1)

      radar.event(a1.get)

      radar.size should === (1)
      radar.expirations.size === (1)

      radar.stop
    }

    s"get 2 events and have size == 1 Aircrafts" in {
      val radar = new Radar
      val a1 = decoder.decode(msg2)
      val a2 = decoder.decode(msg1)

      radar.event(a2.get)
      radar.event(a1.get)

      radar.size should === (1)
      radar.expirations.size === (1)

      radar.stop
    }

    s"expire 1 aircraft and have size == 0 after 2 seconds" in {
      val radar = new Radar(expiryTime = Duration("2 seconds"),expiryCheck = Duration("1 seconds"))
      val a1 = decoder.decode(msg2)
      val a2 = decoder.decode(msg1)

      radar.event(a2.get)
      radar.event(a1.get)

      radar.size should === (1)
      radar.expirations.size === (1)

      Thread.sleep(2000)

      radar.size should === (0)
      radar.expirations.size === (0)

      radar.stop
    }

    s"get ${flight1.size} events for the same Aircraft and have size == 1 Aircrafts" in {
      val radar = new Radar
    
      for( m <- flight1 ) { radar.event(decoder.decode(m).get) }

      radar.size should === (1)
      radar.expirations.size === (1)

      radar.stop
    }

    s"update flight Flight(${flight2.size}) with telemetry" in {
      val radar = new Radar
    
      val a1 = AircraftAddress("508035","Antonov An-225 Mriya","UR-82060")

      for( m <- flight2 ) { 
        radar.event(decoder.decode(m).get)
        val aircraft = radar.find(a1)
        aircraft.isDefined should ===(true)
      }
      val aircraft1 = radar.find(a1)
      aircraft1.get.telemetry.size should ===(8)
      val t1 = aircraft1.get.telemetry.last
      t1 should ===(AircraftTelemetry(id = a1,Location(50.643295,30.186124,Altitude(1275.0,Units.FEET)),Speed(220.435,Units.KNOTS),VRate(-1088.0,Units.FPM),258))

      for( t <- aircraft1.get.telemetry) {
        //info(s"${t}")
      }
      radar.stop
    }
    
  }  
}
