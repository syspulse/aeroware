package io.syspulse.aeroware.adsb.radar.server

import scala.collection.immutable
import spray.json.DefaultJsonProtocol
import spray.json._

import io.syspulse.skel.service.JsonCommon
import io.syspulse.aeroware.adsb.core.AdsbJson
import io.syspulse.aeroware.adsb.radar.TrackTelemetry

final case class RadarTelemetry(data: immutable.Seq[TrackTelemetry],total:Option[Long]=None)

object RadarProto extends JsonCommon {
  import DefaultJsonProtocol._
  import io.syspulse.aeroware.adsb.core.AdsbJson._
  
  implicit val jf_tt = jsonFormat6(TrackTelemetry)

  implicit val jf_rd = jsonFormat2(RadarTelemetry)
  
}