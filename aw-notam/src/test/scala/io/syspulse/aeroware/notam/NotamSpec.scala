package io.syspulse.aeroware.notam

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import java.time.format._
import java.time.temporal._

import io.jvm.uuid._

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.notam._
import io.syspulse.aeroware.notam.Notam._

import scala.util._
import scala.io.Source

import fastparse._, NoWhitespace._
import org.scalactic.TolerantNumerics

class NotamSpec extends AnyWordSpec with Matchers {
  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.0001)
  
  val TEST_DIR = os.Path(this.getClass.getClassLoader.getResource(".").getPath + "/data")
  
  val NOTAM_1 = """
Q) EGXX/QRDCA/IV/BO/W/000/080/5451N00456W014 FROM 07/06/18 14:00 TO 07/06/22 17:00
D) 18 1400-1700 19 - 22 0900-1700
E) DANGER AREA EG D402A ACTIVE ABOVE NORMAL LEVEL
F) SFC
G) 8000FT AMSL"""

val NOTAM_2 = """
A) LFAD COMPIEGNE MARGNY
B) 2007 Jun 01 06:00
C) 2007 Aug 29 23:59
E) VOR/DME CPE 109.65MHZ CH33Y OUT OF SERVICE"""

val NOTAM_3="""
Q) EGTT/QWPLW/IV/M/W/000/130/5217N00255W008 FROM 07/06/18 07:00 TO 07/06/18 23:00
E) AUS 07-06-0531/1977/AS3 EXERCISE BLACK MOUNTAIN.
STATIC LINE AND FREEFALL PJE WI 8NM RADIUS 5217N 00255W (BYTON).
DROP CONE EXTENDS TO 4NM RAD SFC TO 6000FT,8NM RADIUS 6000-12000FT AGL.
ACFT IN DROP CONFIGURATION MAY BE UNABLE TO COMPLY WITH RULES OF THE AIR.
CONTACT 07767 238541.
F) SFC
G)FL130"""

val NOTAM_4  = """
A) EGCC
B) 0108122359
C) 0109112359
D) MON-FRI 0800-2359
E) RWY 26 closed
"""

  "Notam" should {
    "parse 'NOTAM_1'" in {
      val n1 = Notam.decode(NOTAM_1)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }

    "parse 'NOTAM_4'" in {
      val n1 = Notam.decode(NOTAM_4)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }
  }

}
