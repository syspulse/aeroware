package io.syspulse.aeroware.core

import scala.util._

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class AircraftCategorySpec extends AnyWordSpec with Matchers {

  val id1 = "4840d6"
    
  "AircraftCategorySpec" should {
    
    s"Categories size == 4" in {
      AircraftCategory.values.size should === (4)
    }

    s"Category contain 'Airplane'" in {
      AircraftCategory.withName("Airplane") should === (AircraftCategory.Airplane)
      AircraftCategory.withName("Airplane").toString should === ("Airplane$(1,Airplane)")
    }

    s"Category does not contain 'Rocket'" in {
      AircraftCategory.withName("Rocket") should === (AircraftCategory.Unknown)
      AircraftCategory.withName("Rocket").toString should === ("Unknown$(0,Unknown)")
    }

    s"CategoryRepo size is 4" in {
      AircraftCategoryRepo.db.size === (4)
    }
    
    s"match 'Airplane'" in {
      import AircraftCategory._
      val a = AircraftCategoryRepo.db.filter { c => c match {
        case Airplane => true
        case _ => false
      }}

      a === (true)
    }
  }
}
