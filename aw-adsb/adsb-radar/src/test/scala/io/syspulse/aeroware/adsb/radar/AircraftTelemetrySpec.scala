package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import io.syspulse.aeroware.core.Location
import io.syspulse.aeroware.core.SpeedType
import io.syspulse.aeroware.core.VRate
import io.syspulse.aeroware.core.Altitude
import io.syspulse.aeroware.core.Units
import io.syspulse.aeroware.core.Speed

import io.syspulse.aeroware.adsb.radar
import io.syspulse.aeroware.adsb.radar.AircraftTelemetry


class AircraftTelemetrySpec extends AnyWordSpecLike with Matchers {
  val a1 = radar.Aircraft(AircraftAddress("UK-CQF-001","C-172","UR-CQF"))

  val v1 = AircraftTelemetry(a1.id,loc=Location(52.123,3.17,Altitude(100,Units.METERS)),hSpeed=Speed(60,Units.KNOTS),vRate=VRate(5,Units.FPM),heading=0.0)
  val v2 = AircraftTelemetry(a1.id,loc=Location(52.123,3.17,Altitude(100,Units.METERS)),hSpeed=Speed(60,Units.KNOTS),vRate=VRate(5,Units.FPM),heading=0.0)

  val v3 = AircraftTelemetry(a1.id,loc=Location(52.258,3.18,Altitude(150,Units.METERS)),hSpeed=Speed(65,Units.KNOTS),vRate=VRate(5,Units.FPM),heading=0.0)

  "AircraftTelemetrySpec" must {

    s"compare two telemetries ${v1}:${v2}" in {    
      v1 should ===(v2)
    }

    s"NOT compare two telemetries: ${v1}:${v3}" in {    
      v1 should !==(v3)
    }
  }
}