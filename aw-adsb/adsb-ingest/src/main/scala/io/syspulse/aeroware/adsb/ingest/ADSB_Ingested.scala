package io.syspulse.aeroware.adsb.ingest

import io.syspulse.aeroware.adsb.core.{ ADSB }
import io.syspulse.skel.Ingestable

import spray.json._
import AdsbIngestedJsonProtocol._
import io.syspulse.skel.util.Util


case class ADSB_Ingested(adsb:ADSB,format:String="") extends Ingestable {

  override def toString: String = format match {
    case "json" => this.toJson.compactPrint
    case "csv" => Util.toCSV(adsb.asInstanceOf[Product])
    // format used for replay with live timestamp
    case "adsb" => s"${adsb.ts} ${adsb.raw}"
    case _ => super.toString
  }
}

object ADSB_Ingested {
  def apply(adbs:ADSB,format:String = ""):ADSB_Ingested = {
    new ADSB_Ingested(adbs,format)
  }
}