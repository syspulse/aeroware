package io.syspulse.aeroware.gamet

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import java.time.format._
import java.time.temporal._

import io.jvm.uuid._

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.gamet._
import io.syspulse.aeroware.gamet.Gamet._

import scala.util._
import scala.io.Source

import fastparse._, NoWhitespace._

class GametSpec extends AnyWordSpec with Matchers {
  
  val GAMET_EXAMPLES_DIR = os.Path(this.getClass.getClassLoader.getResource(".").getPath + "/data")

  val GAMET_HEADER_EXAMPLE_1 = "FADL41 EDZM 010900"

  val GAMET_EXAMPLE_FAIL_2 = """
FADL41 EDZM 010900
_ EDMM GAMET VALID 010900/011500 EDZM-
EDMM MUNCHEN FIR BLW FL150
"""

  val GAMET_EXAMPLE_1 = """
FADL41 EDZM 010900
EDMM GAMET VALID 010900/011500 EDZM-
EDMM MUNCHEN FIR BLW FL150
SECN I
SIGWX   : 09/12 NW OF LINE EDNY-LKPR ISOL TS
          12/15 ISOL TS
SIG CLD : 09/12 NW OF LINE EDNY-LKPR ISOL CB FL060/XXX
          12/15 ISOL CB FL060/XXX
SIGMET APPLICABLE: AT TIME OF ISSUE NIL
SECN II
PSYS    : 12 N5100 E01545 H 1019HPA STNR NC
          12 N4340 E00500 L 1014HPA STNR NC
          12 W GERMANY UPPER TROUGH OVR MOIST AIRMASS
WIND/T  : 3000FT AMSL N4930 E01200 050/05KT PS25
          5000FT AMSL N4930 E01200 050/05KT PS21
          FL100       N4930 E01200 020/05KT PS08
CLD     : FEW CU FL060/090
FZLVL   : FL135
MNM QNH : 09/11 1018HPA
          11/13 1017HPA
          13/15 1017HPA
VA      : NIL
CHECK GAFOR (VIS AND CLD BASE), AIRMET AND SIGMET-INFORMATION="""

  "SECN" should {
    "parse 'SECN I" in {
      val p = parse("SECN I", Gamet.secnParser(_))
      p should === (Parsed.Success(SECN(1), 6))
    }

    "parse 'SECN II" in {
      val p = parse("SECN II", Gamet.secnParser(_))
      p should === (Parsed.Success(SECN(2), 7))
    }

    "parse 'SECN 1" in {
      val p = parse("SECN 1", Gamet.secnParser(_))
      p should === (Parsed.Success(SECN(1), 6))
    }

    "parse 'SECN 2" in {
      val p = parse("SECN 2", Gamet.secnParser(_))
      p should === (Parsed.Success(SECN(2), 6))
    }

    "parse 'FZLVL   : 13500' to 13500" in {
      val p = parse("FZLVL   : 13500", Gamet.fzlvlParser(_))
      p should === (Parsed.Success(FZLVL(Alt(13500)), 15))
    }
  }

  "FZLVL" should {
    "parse 'FZLVL   : FL135' to 13500" in {
      val p = parse("FZLVL   : FL135", Gamet.fzlvlParser(_))
      p should === (Parsed.Success(FZLVL(FL(135)), 15))
    }
    "parse 'FZLVL   : 13500' to 13500" in {
      val p = parse("FZLVL   : 13500", Gamet.fzlvlParser(_))
      p should === (Parsed.Success(FZLVL(Alt(13500)), 15))
    }
    "parse 'FZLVL   : FL125-130' to 12500,13000" in {
      val p = parse("FZLVL   : FL125-130", Gamet.fzlvlParser(_))
      p should === (Parsed.Success(FZLVL(FL(125,130)), 19))
    }
  }

  "SIGWX" should {
    "parse 'SIGWX   : 09/12 NW OF LINE EDNY-LKPR ISOL TS" in {
      val p = parse("SIGWX   : 09/12 NW OF LINE EDNY-LKPR ISOL TS", Gamet.sigwxParser(_))
      p should === (Parsed.Success(SIGWX("09/12 NW OF LINE EDNY-LKPR ISOL TS"), 44))
    }
  }

  "Header" should {
    "decode 'FADL41 EDZM 010900' successfully" in {
      val g = Gamet.decodeHeader("FADL41 EDZM 010900")
      g should === (Success(Gamet.Header("FA", "DL", 41, "EDZM", ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T09:00:00Z[UTC]"), None)))
    }

    "decode 'FADL41 EDZM 010900 AAA' successfully" in {
      val g = Gamet.decodeHeader("FADL41 EDZM 010900 AAA")
      g should === (Success(Gamet.Header("FA", "DL", 41, "EDZM", ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T09:00:00Z[UTC]"), Some("AAA"))))
    }

    "decode 'FACZ41 LKPW 290900' successfully only for non-leap year and fail for 2023" in {
      val g = Gamet.decodeHeader("FACZ41 LKPW 290900")
      if(! LocalDate.now().isLeapYear)
        assertThrows[java.time.DateTimeException] {
          g should === (Success(Gamet.Header("FA", "CZ", 41, "LKPW", ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-29T09:00:00Z[UTC]"), None)))
        }
      else
        {
          g should === (Success(Gamet.Header("FA", "CZ", 41, "LKPW", ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-29T09:00:00Z[UTC]"), None)))
        }
    }    
  }

  "Line1" should {
    "parse 'EDMM GAMET VALID 010900/011500 EDZM-'" in {
      val p = parse("EDMM GAMET VALID 010900/011500 EDZM-", Gamet.line1Parser(_))
      p should === (Parsed.Success(Gamet.Line1("EDMM","GAMET",ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T09:00Z[UTC]"),ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T15:00Z[UTC]"),"EDZM"), 36))
    }
  }

  "Line2" should {
    val gamet1 = "EDMM MUNCHEN FIR BLW FL150"
    s"decode '${gamet1}'" in {
      val p = Gamet.decodeLine2(gamet1)
      p should === (Success(Gamet.Line2(Some("EDMM"),FIR("MUNCHEN",None),Some(FL(150)))))
    }

    val gamet2 = "UKBV KYIV FIR"
    s"decode '${gamet2}'" in {
      val p = Gamet.decodeLine2(gamet2)
      p should === (Success(Gamet.Line2(Some("UKBV"),FIR("KYIV",None),None)))
    }

    val gamet3 = "PRAHA FIR BLW FL100"
    s"decode '${gamet3}'" in {
      val p = Gamet.decodeLine2(gamet3)
      p should === (Success(Gamet.Line2(None,FIR("PRAHA",None),Some(FL(100)))))
    }

    val gamet4 = "LOVV WIEN FIR / DANUBE AREA BLW FL200"
    s"decode '${gamet4}'" in {
      val p = Gamet.decodeLine2(gamet4)
      p should === (Success(Gamet.Line2(Some("LOVV"),FIR("WIEN",Some("DANUBE AREA")),Some(FL(200)))))
    }

    val gamet5 = "LOVV WIEN FIR / ALPS SOUTH SIDE BLW FL200"
    s"decode '${gamet5}'" in {
      val p = Gamet.decodeLine2(gamet5)
      p should === (Success(Gamet.Line2(Some("LOVV"),FIR("WIEN",Some("ALPS SOUTH SIDE")),Some(FL(200)))))
    }
  }

  "Line2" should {
    val gamet1 = "EDMM MUNCHEN FIR BLW FL150"
    s"parse '${gamet1}'" in {
      val p = decodeLine2(gamet1)
      p should === (Success(Gamet.Line2(Some("EDMM"),FIR("MUNCHEN",None),Some(FL(150)))))
    }

    val gamet2 = "UKBV KYIV FIR"
    s"parse '${gamet2}'" in {
      val p = decodeLine2(gamet2)
      p should === (Success(Gamet.Line2(Some("UKBV"),FIR("KYIV",None),None)))
    }

    val gamet3 = "PRAHA FIR BLW FL100"
    s"parse '${gamet3}'" in {
      val p = decodeLine2(gamet3)
      p should === (Success(Gamet.Line2(None,FIR("PRAHA",None),Some(FL(100)))))
    }

    val gamet4 = "LOVV WIEN FIR / DANUBE AREA BLW FL200"
    s"parse '${gamet4}'" in {
      val p = decodeLine2(gamet4)
      p should === (Success(Gamet.Line2(Some("LOVV"),FIR("WIEN",Some("DANUBE AREA")),Some(FL(200)))))
    }

    val gamet5 = "LOVV WIEN FIR / ALPS SOUTH SIDE BLW FL200"
    s"parse '${gamet5}'" in {
      val p = decodeLine2(gamet5)
      p should === (Success(Gamet.Line2(Some("LOVV"),FIR("WIEN",Some("ALPS SOUTH SIDE")),Some(FL(200)))))
    }
  }

  "Gamet" should {
    "not parse Gamet Message 2" in {
      val g = Gamet.decode(GAMET_EXAMPLE_FAIL_2)
      g.isFailure should === (true)
      info(g.toString)
    }

    "parse Gamet Message 1 Header, Line1, Line2" in {
      val g = Gamet.decode(GAMET_EXAMPLE_1)
      g.isSuccess should === (true)
      
      g.get.header should === (Gamet.Header("FA", "DL", 41, "EDZM", ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T09:00:00Z[UTC]"), None))
      g.get.line1 should === (Gamet.Line1("EDMM","GAMET",ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T09:00Z[UTC]"),ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-01T15:00Z[UTC]"),"EDZM"))
      g.get.line2 should === (Gamet.Line2(Some("EDMM"),FIR("MUNCHEN",None),Some(FL(150))))
      g.get.data.size should === (20)
    }
  }

  "Test data/*.gamet Files" should {

    "be parsed successfully" in {
      os.list(GAMET_EXAMPLES_DIR).filter(_.ext == "gamet").foreach{ gametFileName => 
        
        val gametFile = Source.fromFile(gametFileName.toString).getLines().mkString("\n")
        info(s"${gametFileName.toString}")
        val g = Gamet.decode(gametFile)

        info(s"${gametFileName.toString}: ${g}")

        if(g.isFailure) {
          g match {
            case Failure(e) => 
              e match {
                case ed:java.time.DateTimeException =>
                  if( ! ed.getMessage().contains("is not a leap year"))
                    throw e
                case _  => 
                  throw e
              }
            case _ => 
          }
        } 

        //g.isSuccess should === (true)
      }
      
    }
  }

}
