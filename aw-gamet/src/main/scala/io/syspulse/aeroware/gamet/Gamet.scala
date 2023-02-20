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
  //def time[_: P]:P[ZonedDateTime] = P( timeMonth ~ timeHH ~ timeMM ~ "Z").map( t => tsz(t._1,t._2,t._3))
  //def time[_: P]:P[ZonedDateTime] = P( timeMonth ~ timeHH ~ timeMM).map( t => tsz(t._1,t._2,t._3))
  def time[_: P]:P[ZonedDateTime] = P( timeDay ~ timeHH ~ timeMM).map( t => tsz(t._1,t._2,t._3))


  case class Header(dataType:String,territory:String,number:Int,source:ICAO,ts:ZonedDateTime,correction:Option[String])
  def headerTT[_: P] = P( CharIn("A-Z").rep(exactly=2)).!.map(_.toString) 
  def headerAA[_: P] = P( CharIn("A-Z").rep(exactly=2)).!.map(_.toString)
  def headerII[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def headerCCCC[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def headerAACC[_: P] = P( ("AA" | "CC") ~ CharIn("A-Z")).!.map(_.toString)
  def headerParser[_: P] = P( headerTT ~ headerAA ~ headerII ~ ws ~ headerCCCC ~ ws ~ time ~ (ws ~ headerAACC).? ~ End).map(p => Header(p._1,p._2,p._3,p._4,p._5,p._6))


  case class Line1(firOrg:ICAO,gametId:String,tsStart:ZonedDateTime,tsEnd:ZonedDateTime,meteo:ICAO)
  def line1CCCC1[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString) 
  def line1GAMET[_: P] = P( ("GAMET" ~ (ws ~ ("AMD" | "COR")).?).! ~ ws ~ "VALID").map(_.toString)
  def line1YYGGgg1[_: P] = P( time )
  def line1YYGGgg2[_: P] = P( time )
  def line1CCCC2[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)

  def line1Parser[_: P] = P( line1CCCC1 ~ ws ~ line1GAMET ~ ws ~ line1YYGGgg1 ~ "/" ~ line1YYGGgg2 ~ ws ~ line1CCCC2 ~ "-" ~ End).map(p => Line1(p._1,p._2,p._3,p._4,p._5))


  case class FIR(area:String,subArea:Option[String])
  
  // delimiter: def deimiterParser[_: P] = P((!"FIR" ~ AnyChar).rep.! ~ ("FIR /" | "FIR") ~ AnyChar.rep.!)
  
  case class Line2(firOrg:Option[ICAO],fir:FIR,fl:Option[Altitude])

  def line2CCCC1[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def line2BRWFL[_: P] = P( "BLW" ~ ws ~ Altitude.altParser )

  def firBeforeFirParser[_:P] = P( (!"FIR" ~ AnyChar).rep.! )
  def firSplitterParser[_: P] = P( firBeforeFirParser ~ ("FIR /" | "FIR") ~ AnyChar.rep.!)

  def firParser1[_: P] = P( (line2CCCC1 ~ ws).? ~ CharIn("A-Z0-9").rep.!)
  def firParser2[_: P] = P((!"BLW" ~ CharIn("A-Za-z 0-9")).rep.! ~ ("BLW" ~ ws ~ Altitude.altParser).? )
  
  //def line2Parser[_: P] = P( (line2CCCC1 ~ ws).? ~ firParser ~ (ws ~ line2BRWFL).? ~ End).map(p => Line2(p._1,p._2,p._3))
  
  def decodeLine2(data:String): Try[Line2] = {
    log.debug(s"data: '${data}'")
    val firParts = parse(data.trim, firSplitterParser(_))
    (
      if(firParts.isSuccess) {
        val part1 = firParts.get.value._1
        val part2 = firParts.get.value._2

        val fir1 = parse(part1, firParser1(_))
        if(fir1.isSuccess) {
          val fir2 = parse(part2, firParser2(_))
          if(fir2.isSuccess) {
            val firOrg = fir1.get.value._1
            var area = fir2.get.value._1.trim
            val fir = FIR(fir1.get.value._2, if(area.isEmpty) None else Some(area))
            var blw = fir2.get.value._2
            return Success(Line2(firOrg,fir,blw))
          } else fir2
        } else fir1
      } else firParts
    )
    .fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(Line2(None,FIR("",None),None)))
  }

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
    log.debug(s"header: '${data}'")
    val header = Try {
      parse(data.trim, headerParser(_))
    }
    header.flatMap(header => {
      header.fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(h))
    })
  }

  def decodeLine1(data:String): Try[Line1] = {
    log.debug(s"line1: '${data}'")
    val line1 = Try {
      parse(data.trim, line1Parser(_))
    }
    line1.flatMap(line1 => {
      line1.fold( (s,i,extra)=>Failure(new Exception(s"${s}: pos=${i}: ${extra.input}")), (h,i) => Success(h))
    })
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
        log.warn(s"Unknown data: ${data}")
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


