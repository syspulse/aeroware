package io.syspulse.aeroware.metar

import scala.annotation.nowarn

import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Locale
import io.jvm.uuid._

import fastparse._, NoWhitespace._
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import scala.util.{Try,Success,Failure}
import os.temp

abstract class Metar {
  def describe:String
}

@nowarn("msg=Such classes will overwrite one another on case-insensitive filesystems")
case class METAR(data:MetarData) extends Metar {
  def describe = s"METAR: ${data}"
}

case class SPECI(data:MetarData) extends Metar {
  def describe = s"SPECI: ${data}"
}

case class MetarData(
  stationId:String,
  ts:ZonedDateTime,
  auto:Boolean,
  wind:Metar.Wind,
  windGust:Option[Metar.Wind],
  visibility:Metar.Visibility,
  rvr:Seq[Metar.RVR],
  weather:Seq[Metar.Weather],
  sky:Seq[Metar.Sky],
  temp:Metar.Temperature,
  dew:Metar.Temperature,
  alt:Metar.Altimiter,
  data:List[String]
)

@nowarn("msg=Such classes will overwrite one another on case-insensitive filesystems")
object Metar {
  val log = Logger(s"${this}")

  type ICAO = String
  val utc = ZoneId.of("UTC");

  case class Wind(dir:Int,speed:Int,unit:String,dirVar1:Option[Int] = None,dirVar2:Option[Int] = None)
  case class Visibility(dist:Double,unit:String)
  case class Sky(cloud:String,alt:Option[Int]) {
    def toText() =  {
      val intensity = cloud.headOption
      cloud match {
        case "SKC" => "No cloud/Sky clear"
        case "NCD" => "Nil Cloud detected"
        case "NSC" => "No (nil) significant cloud (none below 5,000 ft (1,500 m) and no TCU or CB)"
        case "CLR" => "no clouds detected below 12000 feet"
        case "FEW" => "few"
        case "SCT" => "scattered"
        case "BKN" => "broken"
        case "OVC" => "overcast"
        case "TCU" => "Towering cumulus cloud"
        case "CB" => "Cumulonimbus cloud"
        case "VV" => "vertical visibility"
        case _ => cloud
      }
      cloud
    }
  }
  case class Temperature(v:Int)
  case class Altimiter(v:Int,unit:String)
  case class RVR(runway:Int,range:Int,unit:String)
  case class Weather(w:String,intensity:Option[String] = None) {
    def toText() =  {      
      val itxt = intensity match {
        case Some("+") => "Heavy intensity"
        case Some("-") => "Light intensity"
        case _ => "Moderate intensity"
      }
      val wtxt = w match {
        case "RA" => "rain"
        case "SN" => "snow" 
        case "UP" => "precipitation of unknown type"
        case "FG" => "fog"
        case "FZFG" => "freezing fog (temperature below 0°C)"
        case "BR" => "mist"
        case "HZ" => "haze"
        case "SQ" => "squall"
        case "FC" => "funnel cloud/tornado/waterspout"
        case "TS" => "thunderstorm"
        case "GR" =>  "hail"
        case "GS" => "small hail; <1/4 inch (graupel)"
        case "FZRA" => "freezing rain"
        case "VA" => "volcanic ash"

        case "PR" => "partial (fog)"
        case "BC" => "patches"
        case "DZ" => "drizzle"
        case "DR" => "low drifting below eye level"
        case "BL" => "blowing at or above eye level;"
        case "SH" => "showers"
        case "SG" => "snow grains"
        case "PL" => "ice pellets"
        case "IC" => "ice crystals"
        case "DU" => "widespread dust"
        case "FU" => "smoke"
        case "SA" => "sand"
        case "PY" => "spray"
        case "PO" => "dust"
        case "DS" => "dust storm"
        case "SS" => "sand strom"
        
        case _ => w
      }

      s"${itxt} ${wtxt}"
    }
  }

  object Wind {
    def apply(dir:String,speed:String,unit:String,dirVar:Option[(String,String)]):Metar.Wind = 
      new Metar.Wind(dir.toInt,speed.toInt,unit,dirVar.map(_._1.toInt),dirVar.map(_._2.toInt))
  }

  object Visibility {
    def apply(dist:String,unit:String):Metar.Visibility = new Metar.Visibility(dist.toDouble,unit)
  }

  object Sky {
    def apply(code:String,alt:Option[Int]):Metar.Sky = code match {
      case c if c.startsWith("BKN") =>
        new Metar.Sky(c,alt)
      case _ => 
        new Metar.Sky(code,alt)
    }      
  }

  object Temperature {
    def apply(v:String):Metar.Temperature = new Metar.Temperature(v.toInt)
  }
  object Altimiter {
    def apply(v:String,unit:String):Metar.Altimiter = new Metar.Altimiter(v.toInt,unit)
  }
  
  
  // parsing old messages may fail here since no Year information is preserved
  // 290900 -> this will fail for 2023 because it does not have 29-Feb
  def tsz(d:Int,hh:Int,mm:Int,ss:Int=0,msec:Int=0) = { 
    log.debug(s"tsz=${d}/${hh}::${mm}::${ss}.${msec}")
    val now=OffsetDateTime.now(ZoneOffset.UTC); 
    
    ZonedDateTime.of(
      now.getYear, 
      now.getMonth.getValue, 
      d, 
      hh,mm, 
      if(ss == 0) 0 else now.getSecond,
      if(msec == 0) 0 else now.getNano,
      utc ) 
  }

  def ws[_: P] = P( " ".rep(1) )
  def pws[_: P] = P( " ".rep(0) )

  def timeMonth[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeDay[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeHH[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeMM[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)  
  def time[_: P]:P[ZonedDateTime] = P( timeDay ~ timeHH ~ timeMM ~ "Z").map( t => tsz(t._1,t._2,t._3))
  //def time[_: P]:P[ZonedDateTime] = P( timeDay ~ timeHH ~ timeMM).map( t => tsz(t._1,t._2,t._3))
  
  case class MetarReport(firOrg:ICAO,gametId:String,tsStart:ZonedDateTime,tsEnd:ZonedDateTime,meteo:ICAO)
  def metarType[_: P] = P( ("METAR" | "SPECI")).!.map(_.toString)
  def metarICAOStationId[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def metarDate[_: P] = P( time )
  def metarAuto[_: P] = P((ws ~ "AUTO")).!.map( _ => true )
  
  // 24007KT
  // 21016G24KT 180V240
  def metarWindDir[_: P] = P( CharIn("0-9").rep(exactly=3)).!.map(_.toString)
  def metarWindSpeed[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toString)
  def metarWindSpeedGust[_: P] = P( CharIn("0-9").rep(exactly=2) ~ "G").!.map(_.toString)  
  def metarWind[_: P] = metarWindDir ~ metarWindSpeedGust.? ~ metarWindSpeed ~ P("KT" | "MPS").!
  def metarWindVariable[_: P] = metarWindDir ~ "V" ~ metarWindDir
  
  def metarVisDist[_: P] = P( ("M1/4" | CharIn("0-9").rep(min=1,max=6)) ).!.map(_.toString)
  def metarVisibility[_: P] = metarVisDist ~ P("SM" | "").!

  // Runway visual Range
  // R04/P1500N 
  // R22/P1500U
  def metarRunway[_: P] = "R" ~ P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def metarRunwayRange[_: P] = "P" ~ P(CharIn("0-9").rep(exactly=4)).!.map(_.toInt) ~ P("N" | "U" | "FT").!
  def metarRVR[_: P] = metarRunway ~ "/" ~ metarRunwayRange

  // +SN
  //def metarWeatherPhenomenon[_: P] = P( ("+" | "-").? ~ ("RA" | "SN" | "UP" | "FG" | "FZFG" | "BR" | "HZ" | "SQ" | "FC" | "TS" | "GR" | "GS" | "FZRA" | "VA") ).!.map(_.toString)
  def metarWeatherPhenomenonIntensity[_: P] = ("+" | "-").!
  def metarWeatherPhenomenon[_: P] = metarWeatherPhenomenonIntensity.? ~ P( ("RA" | "SN" | "UP" | "FG" | "FZFG" | "BR" | "HZ" | "SQ" | "FC" | "TS" | "GR" | "GS" | "FZRA" | "VA") ).!.map(_.toString)  
  //def metarWeather[_: P] = (metarWeatherPhenomenon).rep(sep = ws, min = 0)
  def metarWeather[_: P] = metarWeatherPhenomenon

  //def metarSkyCondition[_: P] = P( (("CLR" | "FEW" | "SCT" | "BKN" | "OVC") ~ CharIn("0-9").rep(exactly=3)) | ("VV") ~ CharIn("0-9").rep(min=1,max=6)).!.map(_.toString)
  def metarSkyConditionCloud[_: P] = P("CLR" | "FEW" | "SCT" | "BKN" | "OVC").!.map(_.toString)
  def metarSkyConditionCloudAlt[_: P] = P(CharIn("0-9").rep(exactly=3)).!.map(_.toInt)
  def metarSkyConditionVV[_: P] = P("VV").!.map(_.toString)
  def metarSkyConditionVVAlt[_: P] = P(CharIn("0-9").rep(min=1,max=6)).!.map(_.toInt)
  def metarSkyCondition[_: P] = (metarSkyConditionCloud ~ metarSkyConditionCloudAlt.?) | (metarSkyConditionVV ~ metarSkyConditionVVAlt.?)
  //def metarSky[_: P] = (metarSkyCondition).rep(sep = ws, min = 0)
  def metarSky[_: P] = metarSkyCondition

  def metarTempMinus[_: P] = "M".!.map(_ => -1)
  def metarTempValue[_: P] = P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def metarTemp[_: P] = (metarTempMinus.? ~ metarTempValue).map( v => v._1.getOrElse(1) * v._2)
  def metarTemperatureDew[_: P] = metarTemp ~ "/" ~ metarTemp

  def metarAltemeter[_: P] = ("A" | "Q").! ~ (CharIn("0-9").rep(exactly=4).!.map(_.toInt))

  def metarData[_: P] = P(AnyChar.rep).!.map(_.toString)

  def metarParser[_: P] = P( 
    metarType 
    ~ ws 
    ~ metarICAOStationId 
    ~ ws 
    ~ metarDate 
    ~ metarAuto.? 
    ~ ws 
    ~ metarWind
    ~ (ws ~ metarWindVariable).?
    ~ ws 
    ~ metarVisibility 
    ~ (ws ~ metarRVR).rep().?
    ~ (ws ~ metarWeather).rep().?
    ~ (ws ~ metarSky).rep().?
    ~ ws
    ~ metarTemperatureDew
    ~ ws
    ~ metarAltemeter
    ~ ws
    ~ metarData 
    ~ End)
    
    .map(p => {
      val md = MetarData(
        stationId = p._2,
        ts = p._3,
        auto = p._4.getOrElse(false),
        wind = Wind(p._5._1,p._5._3,p._5._4,p._6),
        windGust = p._5._2.map(w => Wind(p._5._1,w,p._5._4,None)),
        visibility = Visibility(p._7._1,p._7._2),
        rvr = p._8.map(_.map(r => RVR(r._1,r._2._1,r._2._2))).getOrElse(Seq()),
        weather = p._9.map(_.map(w => Weather(w._2,w._1))).getOrElse(Seq()),
        sky = p._10.map(_.map(sk => Sky(sk._1,sk._2))).getOrElse(Seq()),
        temp = Temperature(p._11._1),
        dew = Temperature(p._11._2),
        alt = Altimiter(p._12._2,p._12._1),
        data = p._13.split("\\s+").toList  
      )
      p._1 match {
        case "METAR" => METAR(md)
        case "SPECI" => SPECI(md)
      }          
  })

  def clean(data:String) =  
    data
      .replaceAll(" (//)+"," ")
      .replaceAll("\\s+"," ")

  def decode(data:String): Try[METAR] = {
    log.debug(s"data='${data}'")
    val data1 = clean(data)
    val metar = parse(data1, Metar.metarParser(_))
    metar match {
      case Parsed.Success(v, index) => 
        Success(v.asInstanceOf[METAR])
      case f:Parsed.Failure => Failure(new Exception(f.toString))
    }
  }
}


