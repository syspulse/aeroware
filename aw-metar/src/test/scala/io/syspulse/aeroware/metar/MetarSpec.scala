package io.syspulse.aeroware.metar

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import java.time.format._
import java.time.temporal._

import io.jvm.uuid._

import io.syspulse.skel.util.Util

import scala.util._
import scala.io.Source

import fastparse._, NoWhitespace._
import org.scalactic.TolerantNumerics

class MetarSpec extends AnyWordSpec with Matchers {
  implicit val doubleEquality = TolerantNumerics.tolerantDoubleEquality(0.0001)
  
  val METAR_EXAMPLES_DIR = os.Path(this.getClass.getClassLoader.getResource(".").getPath + "/data")

  val METAR_2 = "METAR KATL 191152Z 11005KT 10SM BKN019 BKN150 07/M01 A3034 RMK AO2 SLP276 T00671006 10072 20061 51013 $"
  val METAR_3 = "METAR KDCA 191152Z 18006KT 10SM SCT200 OVC250 04/M02 A3035 RMK AO2 SLP278 T00391022 10044 20028 55005 $"
  val METAR_4 = "METAR KDEN 191153Z 00000KT 10SM CLR M03/M11 A2985 RMK AO2 SLP107 T10331111 10044 21033 53000"
  val METAR_5 = "METAR KSEA 191153Z 18003KT 4SM -RA BR SCT004 BKN009 OVC013 06/04 A3024 RMK AO2 RAB47 SLP247 P0000 60000 70003 T00560044 10067 20044 55002"

  // US
  val METAR_1 = "METAR KJFK 190851Z 24007KT          10SM                           BKN250 01/M04         A3038 RMK AO2 SLP288 T00111039 50000"
  // ICAO
  val METAR_6 = "METAR LBBG 041600Z 12012MPS 090V150 1400 R04/P1500N R22/P1500U +SN BKN022 OVC050 M04/M07 Q1020 NOSIG 8849//91="

  "Metar" should {
    s"decode US METAR:'${METAR_1}" in {
      val g = Metar.decode(METAR_1)
      g should === (Success(
        METAR(MetarData("KJFK", ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-19T08:51:00Z[UTC]"),false,
          Metar.Wind(240,7,"KT"),
          None,
          Metar.Visibility(10.0,"SM"),
          Seq(),
          Seq(),
          Seq(Metar.Sky("BKN",Some(250))),
          Metar.Temperature(1),
          Metar.Temperature(-4),
          Metar.Altimiter(3038,"A"),
          List("RMK", "AO2", "SLP288", "T00111039", "50000")))
      ))
    }

    s"decode ICAO METAR:'${METAR_6}" in {
      val g = Metar.decode(METAR_6)
      // g should === (Success(
      //   METAR(MetarData(
      //     "LBBG", 
      //     ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-04T16:00Z[UTC]"),
      //     false,
      //     Metar.Wind(120,12,"MPS",Some(90),Some(150)),
      //     None,
      //     Metar.Visibility(1400.0,""),
      //     Seq(Metar.RVR(4,1500,"N"),Metar.RVR(22,1500,"U")),
      //     Seq(Metar.Weather("+SN")),
      //     Seq(Metar.Sky("BKN",22),Metar.Sky("OVC",50)),
      //     Metar.Temperature(-4),
      //     Metar.Temperature(-7),
      //     Metar.Altimiter(1020,"Q"),
      //     List("NOSIG","8849//91=")))
      // ))
      val m = g.get.data
      m.stationId should === ("LBBG")
      m.ts should === (ZonedDateTime.parse(s"${Util.tsToStringYearMonth()}-04T16:00Z[UTC]"))
      m.auto should === (false)
      m.wind should === (Metar.Wind(120,12,"MPS",Some(90),Some(150)))
      m.windGust should === (None)
      m.visibility should === (Metar.Visibility(1400.0,""))
      m.rvr should === (Seq(Metar.RVR(4,1500,"N"),Metar.RVR(22,1500,"U")))
      m.weather should === (Seq(Metar.Weather("SN",Some("+"))))
      m.sky should === (Seq(Metar.Sky("BKN",Some(22)),Metar.Sky("OVC",Some(50))))
      m.temp should === (Metar.Temperature(-4))
      m.dew should === (Metar.Temperature(-7))
      m.alt should === (Metar.Altimiter(1020,"Q"))
      m.data should === (List("NOSIG","8849//91="))
   
    }

    s"decode US METAR:'${METAR_2}" in {
      val g = Metar.decode(METAR_2)
      info(s"${g}")      
    }

    s"decode US METAR:'${METAR_3}" in {
      val g = Metar.decode(METAR_3)
      g shouldBe a[Success[_]]
      info(s"${g}")      
    }

    s"decode US METAR:'${METAR_4}" in {
      val g = Metar.decode(METAR_4)
      g shouldBe a[Success[_]]
      info(s"${g}")      
    }

    s"decode US METAR:'${METAR_5}" in {
      val g = Metar.decode(METAR_5)
      g shouldBe a[Success[_]]
      info(s"${g}")      
    }
    
  }

  // "Test data/*.metar Files" should {

  //   "be parsed successfully" in {
  //     os.list(METAR_EXAMPLES_DIR).filter(_.ext == "metar").foreach{ gametFileName => 
        
  //       val gametFile = Source.fromFile(gametFileName.toString).getLines().mkString("\n")
  //       info(s"${gametFileName.toString}")
  //       val g = Gamet.decode(gametFile)

  //       info(s"${gametFileName.toString}: ${g}")

  //       if(g.isFailure) {
  //         g match {
  //           case Failure(e) => 
  //             e match {
  //               case ed:java.time.DateTimeException =>
  //                 if( ! ed.getMessage().contains("is not a leap year"))
  //                   throw e
  //               case _  => 
  //                 throw e
  //             }
  //           case _ => 
  //         }
  //       } 

  //       //g.isSuccess should === (true)
  //     }
      
  //   }
  // }

}
