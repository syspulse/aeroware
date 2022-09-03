package io.syspulse.aeroware.adsb.ingest

import io.syspulse.aeroware.adsb.core.{ ADSB }
import io.syspulse.skel.Ingestable

import spray.json._
import AdsbIngestedJsonProtocol._

object CSV {
  implicit class CSVPrinter(val prod: Product) extends AnyVal {
    def toCSV: String = prod.productIterator.map{
      case p: Product =>  p.toCSV
      case rest => rest
    }.mkString(",")
  }

  def toCSV(prod: Product) = prod.toCSV
}

case class ADSB_Ingested(adsb:ADSB,format:String="") extends Ingestable {
  import CSV._

  override def toString: String = format match {
    case "json" => this.toJson.compactPrint
    case "csv" => CSV.toCSV(adsb.asInstanceOf[Product])
    case _ => super.toString
  }
}

object ADSB_Ingested {
  def apply(adbs:ADSB,format:String = ""):ADSB_Ingested = {
    new ADSB_Ingested(adbs,format)
  }
}