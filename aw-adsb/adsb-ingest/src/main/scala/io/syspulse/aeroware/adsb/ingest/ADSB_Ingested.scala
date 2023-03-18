package io.syspulse.aeroware.adsb.ingest

import io.syspulse.aeroware.adsb.core.{ ADSB }
import io.syspulse.skel.Ingestable

import spray.json._
import AdsbIngestedJsonProtocol._
import io.syspulse.skel.util.Util

trait ADSB_IngestedFormatter extends Serializable {
  def format(a:ADSB_Ingested):String
}

class ADSB_IngestedFormatterJson extends ADSB_IngestedFormatter {
  def format(a:ADSB_Ingested) = a.toJson.compactPrint
}

class ADSB_IngestedFormatterCsv extends ADSB_IngestedFormatter {
  def format(a:ADSB_Ingested) = a.toCSV
}

class ADSB_IngestedFormatterRaw extends ADSB_IngestedFormatter {
  def format(a:ADSB_Ingested) = s"${a.adsb.ts} ${a.adsb.raw}"
}

case class ADSB_Ingested(adsb:ADSB,format:String) extends Ingestable {
//case class ADSB_Ingested(adsb:ADSB)(implicit formatter:ADSB_IngestedFormatter) extends Ingestable {
//case class ADSB_Ingested(adsb:ADSB) extends Ingestable {
  override def toLog: String = format match {
    case "json" => this.toJson.compactPrint
    // it will still print trailing comma
    case "csv" => this.copy(format="").toCSV.stripSuffix(",")
    // format used for replay with live timestamp
    case "raw" => s"${adsb.ts} ${adsb.raw}"
    case _ => toString
  }
  // override def toLog: String = formatter.format(this)
}

object ADSB_Ingested {
  //implicit val formatter = new ADSB_IngestedFormatterJson 

  def apply(adbs:ADSB,format:String):ADSB_Ingested = {    
    new ADSB_Ingested(adbs,format)
  }
}