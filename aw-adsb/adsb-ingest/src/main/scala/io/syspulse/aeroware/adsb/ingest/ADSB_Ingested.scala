package io.syspulse.aeroware.adsb.ingest

import io.syspulse.aeroware.adsb.core.Decoder

import enumeratum._
import enumeratum.values._

import io.syspulse.aeroware.core.{ Units, Altitude, Location, Speed, VRate}
import io.syspulse.aeroware.adsb.core.{ ADSB }
import io.syspulse.skel.Ingestable

case class ADSB_Ingested(adsb:ADSB) extends Ingestable

object ADSB_Ingested {
  def apply(adbs:ADSB):ADSB_Ingested = {
    new ADSB_Ingested(adbs)
  }
}