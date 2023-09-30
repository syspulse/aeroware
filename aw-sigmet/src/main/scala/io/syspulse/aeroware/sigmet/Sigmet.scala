package io.syspulse.aeroware.sigmet

import java.time._
import java.time.format._
import java.time.temporal._
import java.util.Locale
import io.jvm.uuid._

import fastparse._, NoWhitespace._
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import scala.util.{Try,Success,Failure}

import io.syspulse.aeroware.core.Altitude

// SIGMET is undoubtly the most stupid spec ever
object Sigmet {
  val log = Logger(s"${this}")

  type ICAO = String
  val utc = ZoneId.of("UTC");

  def ws[_: P] = P( " ".rep(1) )
  def pws[_: P] = P( " ".rep(0) )
  def NewLine[_: P] = P( " ".rep(0) ~ "\n" )

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

  def timeMonth[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeDay[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeHH[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeMM[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)  
  def time[_: P]:P[ZonedDateTime] = P( timeDay ~ timeHH ~ timeMM).map( t => tsz(t._1,t._2,t._3))
  
  def headerId[_: P] = P( ("WC"|"WS"|"WV") ~ CharIn("A-Z").rep(exactly=2) ~ CharIn("0-9").rep(exactly=2)).!.map(_.toString)
  def icaoId[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def correction[_: P] = P( "CC" ~ CharIn("AB")).!.map(_.toString)
  def header[_: P] = P(headerId ~ ws ~ icaoId ~ ws ~ time ~ (ws ~ correction).?)

  def icaoATS[_: P] = P( CharIn("A-Z")).rep(exactly=4).!.map(_.toString)
  def daySeqNum[_: P] = P( CharIn("A-Z").rep(max=2).!.map(_.toString)).? ~ P(CharIn("0-9")).!.map(_.toInt)
  //def daySeqNum[_: P] = P(CharIn("0-9")).!.map(_.toInt)
  def timeGroup[_: P] = time ~ "/" ~ time
  def icaoMWO[_: P] = P( CharIn("A-Z")).rep(4).!.map(_.toString) ~ "-"
  
  def line1[_: P] = P(icaoATS ~ ws ~ "SIGMET" ~ ws ~ daySeqNum ~ ws ~ "VALID" ~ ws ~ timeGroup ~ ws ~ icaoMWO )

  def fir[_: P] = 
    P( CharIn("A-Z").rep(exactly=4).!.map(_.toString)) ~ 
    P( ws ~ CharIn("A-Z").rep ~ ws ).!.map(_.toString).? ~ 
    ("FIR" | "FIR/UIR" | "UIR" | "CTA")

  // """(OBSC|EMBD|FRQ|SQL|SEV|FZRA|HVY|VA CLD|RDOACT CLD)\s?(TSGR|TS|TURB|ICE|MTW)?""".r
  def phenomenon[_: P] = 
    P( ("OBSC" | "EMBD" | "FRQ" | "SQL" | "SEV" | "FZRA" | "HVY" | "VA CLD" | "RDOACT CLD") ~ ws ).!.map(_.toString).? ~
    P( ("TSGR" | "TS" | "TURB" | "ICE" | "MTW").!.map(_.toString)).? 


  // ("""(OBS|FCST)""".r)~opt("""AT [0-9]{4,6}Z""".r) 
  def observe[_: P] = 
    P("OBS" | "FCST").!.map(_.toString) ~ ("AT" ~ ws ~ P(CharIn("0-9").rep(max=6)).!.map(_.toString) ~ "Z").?

  
  def locationDir[_: P] = 
    //P("NSEW".rep(min=1,max=3)).!.map(_.toString) 
    P("NSEW".rep(min=1,max=3))
  
  def locationPoint[_: P] = 
    (P("NSEW").!.map(_.toString) ~ P(CharIn("0-9").rep(min=2,max=5)).!.map(_.toInt)).map{ case(nsew,value) => Location(nsew,value)}
  
  def locationVector[_: P] = 
    locationPoint ~ ws ~ locationPoint ~ (ws | NewLine) ~ "-" ~ ws

  //([NSEW]{1,3}\s|(WI(THIN)?\s)?(AREA\s)?([0-9]{2}\s?NM\s)?([NSEW]{1,3}\s)?)?(OF\s)?((LINE|ETNA)\s)?\(?([NSEW][0-9]{2,5}|-|AND)\)?    
  def area[_: P] = 
    //(((locationDir ~ ws) | (("WI" | "WITHIN") ~ ws)) ~ ("AREA" ~ ws).? ~ ( CharIn("A-Z").rep(2) ~ ws ~ "NM" ~ ws)).? ~
    (("WI" | "WITHIN") ~ ws).? ~
    P(locationVector.rep)

  def info[_ : P] = fir ~ ws ~ phenomenon ~ ws ~ observe ~ ws ~ area //~ altitude.? ~ movement.? ~ change.?

  case class Location(nsew:String,value:Int)

  case class Header(
    id:String,
    icao:String,
    time:ZonedDateTime,
    corr:Option[String] = None
  )

  case class SIGMET(
    header:Option[Header],
    icaoATS:String,
    daySeqNum:(Option[String],Int),
    //daySeqNum:Int,
    timeGroup:(ZonedDateTime,ZonedDateTime),
    icaoMWO:String,
    area:Seq[Location] = Seq()
  )

  def parser[_: P] = P( 
      header.?
    ~ (ws | NewLine) 
    ~ line1
    ~ (ws | NewLine) 
    ~ info
    ~ End
    ).map{ case(header,line1,info) =>
      SIGMET(
        header.map(h => Header(h._1,h._2,h._3,h._4)),
        icaoATS = line1._1,
        daySeqNum = line1._2,
        timeGroup = line1._3,
        icaoMWO = line1._4,

        area = info._5.flatMap(ll => Seq(ll._1,ll._2))
      )
    }
  

  def clean(data:String) = {
    data
      .trim      
  }
      
  def decode(data:String): Try[SIGMET] = {
    log.debug(s"data='${data}'")
    val data1 = clean(data)    
        
    parse(data1, Sigmet.parser(_)) match {
      case Parsed.Success(sm, index) => 
        Success(sm)
      case f:Parsed.Failure => 
        Failure(f.get)
    }    
  }
}


