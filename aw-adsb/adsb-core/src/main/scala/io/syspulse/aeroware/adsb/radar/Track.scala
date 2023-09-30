package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import scala.collection.mutable

import com.typesafe.scalalogging.Logger
import io.syspulse.aeroware.adsb.core.AircraftAddress

case class Track(craft:Craft,ts0:Long,ts1:Long,track:Seq[TrackTelemetry])
