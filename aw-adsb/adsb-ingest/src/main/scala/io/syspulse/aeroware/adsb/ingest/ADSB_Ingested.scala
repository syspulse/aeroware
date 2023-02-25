package io.syspulse.aeroware.adsb.ingest

import io.syspulse.aeroware.adsb.core.{ ADSB }
import io.syspulse.skel.Ingestable

import spray.json._
import AdsbIngestedJsonProtocol._
import io.syspulse.skel.util.Util

case class ADSB_Ingested(adsb:ADSB,format:String="") extends Ingestable {

  override def toLog: String = format match {
    case "json" => this.toJson.compactPrint
    case "csv" => toCSV //Util.toCSV(adsb.asInstanceOf[Product])
    // format used for replay with live timestamp
    case "raw" => s"${adsb.ts} ${adsb.raw}"
    case _ => toString
  }
}

object ADSB_Ingested {
  def apply(adbs:ADSB,format:String = ""):ADSB_Ingested = {
    new ADSB_Ingested(adbs,format)
  }
}