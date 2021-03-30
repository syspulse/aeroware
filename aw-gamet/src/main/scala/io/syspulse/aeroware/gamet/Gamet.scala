package io.syspulse.aeroware.gamet

import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Locale
import io.jvm.uuid._

import fastparse._, NoWhitespace._
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import scala.util.{Try,Success,Failure}

import io.syspulse.aeroware.gamet.Altitude._

object Gamet {
  val log = Logger(s"${this}")

  type ICAO = String
  val utc = ZoneId.of("UTC");
  def tsz(m:Int,hh:Int,mm:Int,ss:Int=0,msec:Int=0) = { 
    val now=OffsetDateTime.now(ZoneOffset.UTC); 
    ZonedDateTime.of(
      now.getYear, now.getMonth.getValue, m, 
      hh,mm, 
      if(ss == 0) 0 else now.getSecond,
      if(msec == 0) 0 else now.getNano,
      utc ) 
  }

  def ws[_: P] = P( " ".rep(1) )

  def timeMonth[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeMM[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeHH[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  //def time[_: P]:P[ZonedDateTime] = P( timeMonth ~ timeHH ~ timeMM ~ "Z").map( t => tsz(t._1,t._2,t._3))
  def time[_: P]:P[ZonedDateTime] = P( timeMonth ~ timeHH ~ timeMM).map( t => tsz(t._1,t._2,t._3))


  case class Header(dataType:String,territory:String,number:Int,source:ICAO,ts:ZonedDateTime,correction:Option[String])
  def headerTT[_: P] = P( CharIn("A-Z").rep(exactly=2)).!.map(_.toString) 
  def headerAA[_: P] = P( CharIn("A-Z").rep(exactly=2)).!.map(_.toString)
  def headerII[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def headerCCCC[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def headerAACC[_: P] = P( ("AA" | "CC") ~ CharIn("A-Z")).!.map(_.toString)
  def headerParser[_: P] = P( headerTT ~ headerAA ~ headerII ~ ws ~ headerCCCC ~ ws ~ time ~ (ws ~ headerAACC).? ~ End).map(p => Header(p._1,p._2,p._3,p._4,p._5,p._6))


  case class Line1(fir:ICAO,gametId:String,tsStart:ZonedDateTime,tsEnd:ZonedDateTime,meteo:ICAO)
  def line1CCCC1[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString) 
  def line1GAMET[_: P] = P( ("GAMET" ~ (ws ~ ("AMD" | "COR")).?).! ~ ws ~ "VALID").map(_.toString)
  def line1YYGGgg1[_: P] = P( time )
  def line1YYGGgg2[_: P] = P( time )
  def line1CCCC2[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)

  def line1Parser[_: P] = P( line1CCCC1 ~ ws ~ line1GAMET ~ ws ~ line1YYGGgg1 ~ "/" ~ line1YYGGgg2 ~ ws ~ line1CCCC2 ~ "-" ~ End).map(p => Line1(p._1,p._2,p._3,p._4,p._5))


  case class Line2(fir:Option[ICAO],firName:String,fl:Option[Altitude])
  def line2CCCC1[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def line2FIR[_: P] = P( CharIn("A-Z").rep.! ~ ws ~ "FIR").map(_.toString)
  def line2BRWFL[_: P] = P( "BLW" ~ ws ~ Altitude.altParser )

  def line2Parser[_: P] = P( (line2CCCC1 ~ ws).? ~ line2FIR ~ (ws ~ line2BRWFL).? ~ End).map(p => Line2(p._1,p._2,p._3))

  abstract class GametData {
    def describe:String
  }

  case class UNKNOWN(data:String) extends GametData {
    def describe = s"Unknown/Unparsed: '${data}'"
  }

  case class SECN(section:Int) extends GametData {
    def describe = s"Section ${section}"
  }

  case class FZLVL(alt:Altitude) extends GametData {
    def describe = s"Freezing Level: ${alt}"
  }

  case class SIGWX(data:String) extends GametData {
    def describe = "Significant weather thunderstorm"
  }
  case class SIGCLD(data:String) extends GametData {
    def describe = "Significant cloud"
  }
  case class PSYS(data:String) extends GametData {
    def describe = "Pressure systems"
  }

  // Preprocess step combines complex types (multiple lines) into one string for easy parsing
  def preprocess(gamet:String): Seq[String] = { 
      val lines = gamet.split("\\n").filter(_.size!=0).toSeq
      var prefix = ""
      val output = for (line <- lines ) yield {
          // DON'T TRIM here
          val data = line
          data match {
              case s if(s.trim.startsWith("SIGWX")) => { prefix = "SIGWX: "; data} 
              case s if(s.startsWith("SIG CLD")) => { prefix = "SIG CLD: "; data} 
              case s if(s.startsWith("PSYS")) => { prefix = "PSYS: "; data} 
              case s if(s.startsWith("WIND/T")) => { prefix = "WIND/T: "; data} 
              case s if(s.startsWith("MNM QNH")) => { prefix = "MNM QNH: "; data} 
              case s if(s.startsWith(" ")) => prefix + data
              case _ => { prefix = ""; data.trim }
          }
      }
      output
  }

  def secnParser[_: P] = P( "SECN" ~ ws ~ ( CharIn("I").rep(min=1,max=2) | (CharIn("1-9").rep(1))).! ~ End).map(p => {
    p match {
      case "I" => SECN(1)
      case "II" => SECN(2)
      case _ => SECN(p.toInt)
    }
  })

  def fzlvlParser[GametData: P] = P( "FZLVL" ~ ws ~ ":" ~ ws ~ Altitude.altParser ~ End).map(p => FZLVL(p))
  
  def sigwxParser[GametData: P] = P( "SIGWX" ~ ws ~ ":" ~ ws ~ AnyChar.rep(1).!).map(p => SIGWX(p.toString))


  val parsers = Map(
    "SECN" -> ((msg:String) => parse(msg, secnParser(_))),
    "FZLVL" -> ((msg:String) => parse(msg, fzlvlParser(_))),
    "SIGWX" -> ((msg:String) => parse(msg, sigwxParser(_)))
  )

  case class GAMET(
    header:Header,
    line1: Line1,
    line2: Line2,
    data: Seq[GametData] = Seq()
  )

  def decodeHeader(data:String): Try[Header] = {
    log.debug(s"data: '${data}'")
    val header = parse(data.trim, headerParser(_))
    header.fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(h))
  }

  def decodeLine1(data:String): Try[Line1] = {
    log.debug(s"data: '${data}'")
    val line1 = parse(data.trim, line1Parser(_))
    line1.fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(h))
  }

  def decodeLine2(data:String): Try[Line2] = {
    log.debug(s"data: '${data}'")
    val line2 = parse(data.trim, line2Parser(_))
    line2.fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(h))
  }

  def decode(data:String): Try[GAMET] = {
    val gamet = preprocess(data)
    
    if(gamet.size<1) return Failure(new Exception(s"Header is missing: ${data}"))
    if(gamet.size<2) return Failure(new Exception(s"Line1 is missing: ${data}"))
    if(gamet.size<3) return Failure(new Exception(s"Line2 is missing: ${data}"))

    val header = decodeHeader(gamet(0))
    val line1 = decodeLine1(gamet(1))
    val line2 = decodeLine2(gamet(2))

    val gametData = for (line <- gamet.drop(3) ) yield {
      val data = line.trim
      val keyword = data.split("\\s")(0).toUpperCase()
      val parser = parsers.get(keyword)
      log.debug(s"line: '${data}': keyword: '${keyword}': parser: ${parser}")

      val gametData:Option[GametData] = if(parser.isDefined) {
        val r = parser.get(data).get
        if(r.isSuccess) 
          Some(r.value)
        else {
          log.error(s"Failed to parse: '${data}': ${r}")
          None
        }
      } else {
        log.warn(s"Unkown data: ${data}")
        Some(UNKNOWN(data))
      }

      gametData
    }

    for {
      h <- header
      l1 <- line1
      l2 <- line2
    } yield
      GAMET(
        header = h,
        line1 = l1,
        line2 = l2,
        data = gametData.flatten
      )
  }
}


