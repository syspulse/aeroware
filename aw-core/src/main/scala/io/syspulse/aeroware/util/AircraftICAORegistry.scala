package io.syspulse.aeroware.util

import io.jvm.uuid._
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.core.AircraftICAO

object AircraftICAORegistry {
  type ICAOId = String
  val log = Logger(this.getClass().getSimpleName())

  protected val registry: Map[ICAOId,AircraftICAO] = fromResource().map(a => a.icao -> a).toMap
  
  def find(icao:ICAOId): Option[AircraftICAO] = registry.get(icao.toLowerCase)

  def size = registry.size

  def fromResource(file:String="aircraft_db.csv"): Seq[AircraftICAO] = {
    log.info(s"Loading from Resource: ${file}")

    val txt = scala.io.Source.fromResource(file).getLines()
    val aa = txt.toSeq.tail.map( s => {
      val (icao,regid,mdl,icaoType,operator) = s.split(",").toList match { 
        case icao::regid::mdl::icaoType::operator::n => (icao.toLowerCase,regid.toUpperCase,mdl,icaoType,operator)
        case icao::regid::mdl::_ => (icao.toLowerCase,regid.toUpperCase,mdl,"","")
        case icao::regid::_ => (icao.toLowerCase,regid.toUpperCase,"","","")
        case icao::_ => (icao.toLowerCase,"","","","")
      }
      AircraftICAO(icao,regid,mdl,icaoType,operator)
    })

    log.info(s"Loaded from Resource: ${file}: ${aa.size}")
    aa
  }
}
