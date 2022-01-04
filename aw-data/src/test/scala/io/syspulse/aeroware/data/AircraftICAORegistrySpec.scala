package io.syspulse.aeroware.data

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class AircraftICAORegistrySpec extends WordSpec with Matchers {

  val id1 = "4840d6"
    
  "AircraftICAOLoaderSpec" should {
    
    s"load file from resources file aircraft_db.csv" in {
      val aa = AircraftICAORegistry.fromResourceFile("data/aircraft_db.csv")
      aa should !== (Seq())
      aa.size should === (148259)
    }

    s"return ${id1} as 'Fokker 70','PH-KZD'" in {
      val a = AircraftICAORegistry.find(id1)
      a should === (Some(AircraftICAO(id1,"PH-KZD","f70","Fokker 70","KLM Cityhopper")))

    }

    s"return ${id1.toUpperCase} as 'Fokker 70','PH-KZD'" in {
      val a = AircraftICAORegistry.find(id1.toUpperCase)
      a should === (Some(AircraftICAO(id1,"PH-KZD","f70","Fokker 70","KLM Cityhopper")))
    }

    s"load file from resources file BasicAircraftLookup.csv" in {
      val aa = AircraftICAORegistry.fromResourceFile("data/BasicAircraftLookup.csv")
      aa should !== (Seq())
      aa.size should === (31106)
    }

    s"load file from resources file flightaware.csv" in {
      val aa = AircraftICAORegistry.fromResourceFile("data/flightaware.csv")
      aa should !== (Seq())
      aa.size should === (1576442)
    }

    s"have default DB loaded (small legacy db)" in {
      val sz = AircraftICAORegistry.size
      sz should === (148259)
    }

    s"sync all DB loaded (3 datasets)" in {
      AircraftICAORegistry.sync()
      val sz = AircraftICAORegistry.size
      sz should === (1613032)
    }

    s"return all aircraft from All Datasets" in {
      AircraftICAORegistry.find("508035") should !== (None)
      AircraftICAORegistry.find("3CCDBB") should !== (None)
      AircraftICAORegistry.find("3ccDBB") should !== (None)

      val a = AircraftICAORegistry.find(id1)
      a should === (Some(AircraftICAO("4840D6","PH-KZD","","F70","")))
      
    }
  }
}
