package io.syspulse.aeroware.adsb.radar.store

import scala.util.Try
import scala.concurrent.Future
import scala.collection.immutable

import io.jvm.uuid._
import io.syspulse.aeroware.adsb.radar.Trackable
import io.syspulse.aeroware.adsb.core.AircraftAddress

import io.syspulse.aeroware.adsb.mesh.MeshData
import io.syspulse.aeroware.adsb.radar.TrackTelemetry

trait RadarStore {
  
  def +(t:TrackTelemetry):Future[Try[RadarStore]]
  
  // ingest data from validator
  def <--(d:MeshData):Future[Try[RadarStore]]
  
  def ??(addr:AircraftAddress,ts0:Long,ts1:Long):Future[Try[Seq[TrackTelemetry]]]
  def ??(ts0:Long,ts1:Long):Future[Try[Seq[TrackTelemetry]]]
  //def ??(addr:AircraftAddress):Future[Try[Seq[Craft]]]
  
  def all:Future[Try[Seq[TrackTelemetry]]]

  def size:Long
}

