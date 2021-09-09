package io.syspulse.aeroware.adsb.live

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.core.Location
import io.syspulse.aeroware.core.Speed
import io.syspulse.aeroware.core.VRate

case class AircraftTelemetry(id:AircraftAddress,loc:Location,hSpeed:Speed,vRate:VRate)

