package io.syspulse.aeroware.adsb.ingest

import io.syspulse.aeroware.adsb.core.{ ADSB }
import io.syspulse.skel.Ingestable

import spray.json._
import AdsbIngestedJsonProtocol._
import io.syspulse.skel.util.Util

case class ADSB_Ingested(adsb:ADSB) extends Ingestable {
}

object ADSB_Ingested {
  def apply(adbs:ADSB):ADSB_Ingested = {    
    new ADSB_Ingested(adbs)
  }
}