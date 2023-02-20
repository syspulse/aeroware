package io.syspulse.aeroware.aircraft.icao

import io.jvm.uuid._
import com.typesafe.scalalogging.Logger

import io.syspulse.skel.util.Util

object AircraftICAORegistry {
  type ICAOId = String
  val log = Logger(this.getClass().getSimpleName())

  protected var registry: Map[ICAOId,AircraftICAO] = {
    (
      fromResourceFile("data/aircraft_db.csv")
    )
    .map(a => a.icao.toLowerCase -> a).toMap
  }

  def sync() = {
    registry = (
      fromResourceFile("data/aircraft_db.csv")
      ++ fromResourceFile("data/BasicAircraftLookup.csv")
      ++ fromResourceFile("data/flightaware.csv")
    )
    .map(a => a.icao.toLowerCase -> a).toMap
  }
  
  def find(icao:ICAOId): Option[AircraftICAO] = registry.get(icao.toLowerCase)

  def size = registry.size

  def fromLegacyDBIterator(txt:Iterator[String]): Seq[AircraftICAO] = {
    txt.toSeq.tail.flatMap( s => {
      s.split(",").toList match {
        case icao::regid::mdl::icaoType::operator::n => Some(AircraftICAO(icao.toLowerCase,regid.toUpperCase,mdl,icaoType,operator))
        case icao::regid::mdl::_ => Some(AircraftICAO(icao.toLowerCase,regid.toUpperCase,mdl,"",""))
        case icao::regid::_ => Some(AircraftICAO(icao.toLowerCase,regid.toUpperCase,"","",""))
        case icao::_ => Some(AircraftICAO(icao.toLowerCase,"","","",""))
        case _ => { log.error(s"unknown format: '${s}'"); None }
      }
    })
  }

  def fromVRSIterator(txt:Iterator[String]): Seq[AircraftICAO] = {
    txt.toSeq.tail.flatMap( s => {
      s.split(",") match { 
        case Array(icao,r,t) => Some(AircraftICAO(icao,r,"",t,""))
        case Array(icao,r) => Some(AircraftICAO(icao,r,"","",""))
        case Array(icao) => Some(AircraftICAO(icao,"","","",""))
        case _ => { log.error(s"unknown format: '${s}'"); None }
      }
    })
  }

  def fromFAWIterator(txt:Iterator[String]): Seq[AircraftICAO] = {
    txt.toSeq.tail.flatMap( s => {
      s.split(",") match { 
        case Array(icao,r,t,desc) => Some(AircraftICAO(icao,r,"",t,desc))
        case Array(icao,r,t) => Some(AircraftICAO(icao,r,"",t,""))
        case Array(icao,r) => Some(AircraftICAO(icao,r,"","",""))
        case Array(icao) => Some(AircraftICAO(icao,"","","",""))
        case _ => { log.error(s"unknown format: '${s}'"); None }
      }
    })
  }

  def fromResourceFile(file:String="data/aircraft_db.csv"): Seq[AircraftICAO] = {
    log.info(s"Loading from Resource: ${file}")

    val txt = scala.io.Source.fromResource(file).getLines()

    // detect file type by name
    val header = txt.take(1).toSeq.head
    log.info(s"Loaded from Resource: ${file}: header='${header}'")

    val aa = header.trim.toLowerCase() match {
      case "icao,regid,mdl,type,operator" => fromLegacyDBIterator(txt)
      case "icao24,r,t" => fromVRSIterator(txt)
      case "icao24,r,t,desc" => fromFAWIterator(txt)

      case _ => { log.error(s"Loading from Resource: ${file}: unknown format: '${header}'"); Seq() }
    }

    log.info(s"Loaded from Resource: ${file}: ${aa.size}")
    aa
  }
  
}
