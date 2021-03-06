package io.syspulse.aeroware.gamet

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import java.time.format._
import java.time.temporal._

import io.jvm.uuid._
import io.syspulse.aeroware.gamet._
import io.syspulse.aeroware.gamet.Altitude._

import scala.util._
import scala.io.Source

import fastparse._, NoWhitespace._

class AltitudeSpec extends WordSpec with Matchers {
  
  val EXAMPLES_DIR = os.Path(this.getClass.getClassLoader.getResource(".").getPath + "/data")


  "Altitude" should {
    "parse 'FL135' to 13500" in {
      val p = parse("FL135", Altitude.altParser(_))
      p should === (Parsed.Success(FL(135), 5))
    }
    "parse '13500' to 13500" in {
      val p = parse("13500", Altitude.altParser(_))
      p should === (Parsed.Success(Alt(13500), 5))
    }
    "parse 'FL125-130' to 12500,13000" in {
      val p = parse("FL125-130", Altitude.altParser(_))
      p should === (Parsed.Success(FL(125,130), 9))
    }
  }


}
