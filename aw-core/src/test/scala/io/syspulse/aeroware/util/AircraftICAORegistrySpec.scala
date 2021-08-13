package io.syspulse.aeroware.util

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.core._

class AircraftICAORegistrySpec extends WordSpec with Matchers {

  val id1 = "4840d6"
    
  "AircraftICAOLoaderSpec" should {
    s"load file DB from resources" in {
      val aa = AircraftICAORegistry.fromResource()
      aa should !== (Seq())
      aa.size should === (148260)
    }

    s"have default DB loaded" in {
      val sz = AircraftICAORegistry.size
      sz should === (148260)
    }

    s"return ${id1} as 'Fokker 70','PH-KZD'" in {
      val a = AircraftICAORegistry.find(id1)
      a should === (Some(AircraftICAO(id1,"PH-KZD","f70","Fokker 70","KLM Cityhopper")))

    }

    s"return ${id1.toUpperCase} as 'Fokker 70','PH-KZD'" in {
      val a = AircraftICAORegistry.find(id1.toUpperCase)
      a should === (Some(AircraftICAO(id1,"PH-KZD","f70","Fokker 70","KLM Cityhopper")))

    }
  }
}
