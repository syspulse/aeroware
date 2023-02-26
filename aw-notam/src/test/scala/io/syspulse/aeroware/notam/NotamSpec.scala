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
G) FL130"""

val NOTAM_4  = """
A) EGCC
B) 0108122359
C) 0109112359
D) MON-FRI 0800-2359
E) RWY 26 closed
"""

val NOTAM_ICAO_1 = """U0038/11 NOTAMN
Q) EDGG/QFAXX/IV/NBO/A /000/999/5002N00834E005
A) EDDF B) 1105240716 C) PERM
E) AD EDDF

GEMIL FLIP VAD
   EDDF 1 AND EDDF - PROCEDURE
   PAGES EDDF 1 AND EDDF 2 ARE SUSPENDED.
   FOR NEW PROCEDURE SEE AIP GERMANY VOLUME VFR EDDF."""

val NOTAM_ICAO_2 = """A0003/23 NOTAMR A0640/22
Q) UKXX/QAFXX/IV/NBO/E /000/999/4748N03112E999
A) UKBV UKDV UKFV UKLV UKOV B) 2301110909 C) 2304102359 EST
E) MILITARY INVASION OF UKRAINE BY THE RUSSIAN FEDERATION. THE USE
OF AIRSPACE OF UKRAINE WITHIN UIR KYIV, FIR LVIV, FIR KYIV,
FIR DNIPRO, FIR ODESA, FIR SIMFEROPOL' IS PROHIBITED FOR ALL AIRCRAFT
EXCEPT STATE AIRCRAFT OF UKRAINE OR UNDER PERMISSION OF GENERAL
STAFF OF THE ARMED FORCES OF UKRAINE. ATS IS NOT PROVIDED"""

val NOTAN_ICAO_3 = """A0024/22 NOTAMR A4338/21
Q) UKBV/QICAS/I /NBO/A /000/999/5036N03012E005
A) UKKM B) 2201050935 C) 2204042359
E) ILS (LOC, GP) RWY 33 OUT OF SERVICE.
REF AIP UKKM AD 2.19, AD 2.24.12-3."""


"Notam" should {
    
    "parse 'NOTAM_1'" in {
      val n1 = Notam.decode(NOTAM_1)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }

    "parse 'NOTAM_2'" in {
      val n1 = Notam.decode(NOTAM_2)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }

    "parse 'NOTAM_3'" in {
      val n1 = Notam.decode(NOTAM_3)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }

    "parse 'NOTAM_4'" in {
      val n1 = Notam.decode(NOTAM_4)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }

    s"parse Line1: 'U0038/11 NOTAMN\n'" in {
      val p = parse("U0038/11 NOTAMN\n", Notam.line_1(_))
      info(s"${p}")
      p.isSuccess should === (true)
      p.get.value should === (NotamID(NotamSeq("U",38,11),"N",None))
    }

    s"parse Line1: 'A0024/22 NOTAMR A4338/21\n'" in {
      val p = parse("A0024/22 NOTAMR A4338/21\n", Notam.line_1(_))
      info(s"${p}")
      p.isSuccess should === (true)
      p.get.value should === (NotamID(NotamSeq("A",24,22),"R",Some(NotamSeq("A",4338,21))))
    }

    "parse 'NOTAM_ICAO_1'" in {
      val n1 = Notam.decode(NOTAM_ICAO_1)
      info(s"${n1}")
      n1.isSuccess should === (true)
    }
  }

}
