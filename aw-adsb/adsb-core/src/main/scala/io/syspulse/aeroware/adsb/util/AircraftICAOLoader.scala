package io.syspulse.aeroware.adsb.util

import io.jvm.uuid._
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.core.AircraftICAO

object AircraftICAOLoader {
  val log = Logger(this.getClass().getSimpleName())
  
  def fromResource(file:String="aircraft_db.csv"): Seq[AircraftICAO] = {
    log.info(s"Loading from Resource: ${file}")

    val txt = scala.io.Source.fromResource(file).getLines()
    val aa = txt.toSeq.map( s => {
      val (icao,regid,mdl,icaoType,operator) = s.split(",").toList match { 
        case icao::regid::mdl::icaoType::operator::n => (icao,regid,mdl,icaoType,operator)
      }
      AircraftICAO(icao,regid,mdl,icaoType,operator)
    })

    log.info(s"Loaded from Resource: ${file}: ${aa.size}")
    aa
  }
}
