package io.syspulse.aeroware.aircraft.icao

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class AircraftICAORegistrySpec extends WordSpec with Matchers {

  val id1 = "4840d6"
    
  "AircraftICAOLoaderSpec" should {

    s"return ${id1} as 'Fokker 70','PH-KZD'" in {
      val a = AircraftICAORegistry.find(id1)
      a should === (Some(AircraftICAO(id1,"PH-KZD","f70","Fokker 70","KLM Cityhopper")))
    }
        
    s"return ${id1.toUpperCase} as 'Fokker 70','PH-KZD'" in {
      val a = AircraftICAORegistry.find(id1.toUpperCase)
      a should === (Some(AircraftICAO(id1,"PH-KZD","f70","Fokker 70","KLM Cityhopper")))
    }

    s"contain 148259 aircrafts" in {
      val a = AircraftICAORegistry.find(id1.toUpperCase)
      AircraftICAORegistry.size should === (148259)
    }

    s"load file from resources file aircraft_db.csv" in {
      val aa = AircraftICAORegistry.fromResourceFile("data/aircraft_db.csv")
      aa should !== (Seq())
      aa.size should === (148259)
    }

    s"fail to load file from resources file UNKNOWN.csv" in {
      val aa = AircraftICAORegistry.fromResourceFile("data/UNKNOWN.csv")
      aa should === (Seq())
      aa.size should === (0)
    }

    // currently removed from resources    
    s"load file from resources file BasicAircraftLookup.csv" ignore {
      val aa = AircraftICAORegistry.fromResourceFile("data/BasicAircraftLookup.csv")
      aa should !== (Seq())
      aa.size should === (31106)
    }

    s"load file from resources file flightaware.csv" ignore {
      val aa = AircraftICAORegistry.fromResourceFile("data/flightaware.csv")
      aa should !== (Seq())
      aa.size should === (1576442)
    }

    s"have default DB loaded (small legacy db)" ignore {
      val sz = AircraftICAORegistry.size
      sz should === (148259)
    }

    s"sync all DB loaded (3 datasets)" ignore {
      AircraftICAORegistry.sync()
      val sz = AircraftICAORegistry.size
      sz should === (1613032)
    }

    s"return all aircraft from All Datasets" ignore {
      AircraftICAORegistry.find("508035") should !== (None)
      AircraftICAORegistry.find("3CCDBB") should !== (None)
      AircraftICAORegistry.find("3ccDBB") should !== (None)

      val a = AircraftICAORegistry.find(id1)
      a should === (Some(AircraftICAO("4840D6","PH-KZD","","F70","")))
      
    }
  }
}
