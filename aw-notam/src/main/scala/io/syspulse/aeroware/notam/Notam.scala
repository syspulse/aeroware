package io.syspulse.aeroware.notam

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

object Notam {
  val log = Logger(s"${this}")

  type ICAO = String
  val utc = ZoneId.of("UTC");

  // parsing old messages may fail here since no Year information is preserved
  // 290900 -> this will fail for 2023 because it does not have 29-Feb
  def tsz(yyyy:Int, month:String, dd:Int,hh:Int,mm:Int,zone:Option[String]=None,ss:Int=0,msec:Int=0) = { 
    log.debug(s"tsz=${yyyy}/${month}/${dd}/${hh}::${mm}::${ss}.${msec}")
    
    val now = OffsetDateTime.now(
      zone match {
        case None => ZoneOffset.UTC
        case Some(z) => ZoneId.of(z, ZoneId.SHORT_IDS)
      }
    )
    
    val MM = Map(
      "Jan" -> 1,
      "Feb" -> 2,
      "Mar" -> 3,
      "Apr" -> 4,
      "May" -> 5,
      "Jun" -> 6,
      "Jul" -> 7,
      "Aug" -> 8,
      "Sep" -> 9,
      "Oct" -> 10,
      "Nov" -> 11,
      "Dec" -> 12,
    )

    ZonedDateTime.of(
      yyyy, 
      MM.get(month).getOrElse( month.toInt ), 
      dd,
      hh,
      mm, 
      if(ss == 0) 0 else now.getSecond,
      if(msec == 0) 0 else now.getNano,
      utc ) 
  }

  def ws[_: P] = P( " ".rep(1) )
  def pws[_: P] = P( " ".rep(0) )
  def NewLine[_: P] = P( " ".rep(0) ~ "\n" )
  //def NewLine[_: P] = P( "\n" )

  def timeYear[_: P] = P( CharIn("0-9").rep(exactly=4)).!.map(_.toInt)
  def timeMonthWord[_: P] = P( CharIn("A-Za-z").rep(exactly=3)).!.map(_.toString)
  def timeMonth[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeDay[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeHH[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  def timeMM[_: P] = P( CharIn("0-9").rep(exactly=2)).!.map(_.toInt)  
  
  //def time[_: P]:P[ZonedDateTime] = P( timeDay ~ timeHH ~ timeMM).map( t => tsz(t._1,t._2,t._3))
  def line_Q[_: P] = "Q)" ~ ws ~ P(CharsWhile(_ != '\n')).!.map(_.toString) ~ NewLine
  
  def line_A_Fir[_: P] = P( CharIn("A-Z").rep(exactly=4)).!.map(_.toString)
  def line_A[_: P] = "A)" ~  
    (      
      ( (ws ~ line_A_Fir.!.map(_.toString.trim)).rep().map(f => (f,None)) ~ NewLine ) |
      ( ws ~ line_A_Fir.!.map(_.toString)).rep(exactly=1) ~ (ws ~ P(CharsWhile(_ != '\n')).!.map(_.toString)).? ~ NewLine
    )
    
  // def line_A[_: P] = "A)" ~ ws ~ line_A_Fir.!.map(_.toString) ~ (ws ~ P(CharsWhile(_ != '\n')).!.map(_.toString)).? ~
  //    (NewLine | ws)
  
  // def line_A[_: P] = "A)" ~ ws ~ 
  //   line_A_Fir.!.map(_.toString) ~ 
  //   (
  //     ((NewLine | End).map(_ => None)) |
  //     (
  //       ws ~ (!("B)" | NewLine | End) ~ AnyChar.rep(1)).!.map(v => Some(v)) ~ &("B)" | NewLine | End)
  //     )
  //   )
  
  def line_DateFormat1[_: P] = 
    P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt) ~  
    P(CharIn("0-9").rep(exactly=2)).!.map(_.toString) ~ 
    P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt) ~ 
    P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt) ~ 
    P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt)

  // format: '0108122359'
  def line_BC_DateFormat1[_: P] = line_DateFormat1
    .map( t => tsz(2000 + t._1, t._2, t._3, t._4, t._5))
  
  // format: '2007 Aug 29 23:59'
  def line_BC_DateFormat2[_: P] = P( timeYear ~ ws ~ timeMonthWord ~ ws ~ timeDay ~ ws ~ timeHH ~ ":" ~ timeMM)
    .map( t => tsz(t._1,t._2,t._3,t._4,t._5) )

  // format: '2304102359 EST'
  def line_BC_DateFormat1_Zone[_: P] = P(line_DateFormat1 ~ ws ~ CharIn("A-Z").rep.!)
    .map( t => tsz(t._1,t._2,t._3,t._4,t._5,Some(t._6)))

  // format: 'PERM'
  def line_BC_DateFormatPerm[_: P] = P( "PERM")
    .map( _ => ZonedDateTime.of(9999,1,1,0,0,0,0,utc) )
    
  def line_B[_: P] = "B)" ~ ws ~ P(line_BC_DateFormat1_Zone | line_BC_DateFormat1 |  line_BC_DateFormat2 | line_BC_DateFormatPerm) ~
    (NewLine)

  def line_C[_: P] = "C)" ~ ws ~ P(line_BC_DateFormat1_Zone | line_BC_DateFormat1 | line_BC_DateFormat2 | line_BC_DateFormatPerm) ~
    (NewLine)


  def line_D[_: P] = "D)" ~ ws ~ P(CharsWhile(_ != '\n')).!.map(_.toString) ~ NewLine
  
  def line_E[_: P] = P( "E)" ~ ws ~ (!("F)" | "G)" | End) ~ AnyChar).rep(1).! ~ &("F)" | "G)" | End) )
  
  def line_F[_: P] = "F)" ~ ws ~ NotamAltitude.altParser ~ (NewLine | End)
  def line_G[_: P] = "G)" ~ ws ~ NotamAltitude.altParser ~ (NewLine | End)


// The new format is SNNNN/YY where:
// S represents the series letter (see Series usage for ICAO NOTAM format).
// NNNN is a four-digit NOTAM number to identify the continuity number, followed by a stroke character (/).
// Each series starts on January 1st at 0000UTC of each year with number 0001. 
// ICAO NOTAM numbers are assigned sequentially from 0001 to 9999.
// /YY is the two digits for the calendar year.
// For example: N0035/19
// NOTAM{x}  x ==  type of NOTAM:
//   N - New
//   R - Replace, 
//   C - Cancel.
// Replace:
//  A0024/22 NOTAMR A4338/21
// New:
//  U0038/11 NOTAMN
  def line_1_SerIdSeq[_: P] = P(
    P(CharIn("A-Z").rep(exactly=1)).!.map(_.toString) ~
    P(CharIn("0-9").rep(exactly=4)).!.map(_.toInt) ~ 
    "/" ~
    P(CharIn("0-9").rep(exactly=2)).!.map(_.toInt)
  ).map( d => NotamSeq(d._1,d._2,d._3))

  def line_1[_: P] = P(
    line_1_SerIdSeq ~
    ws ~
    "NOTAM" ~
    P(CharIn("A-Z").rep(exactly=1)).!.map(_.toString) ~
    (ws ~ line_1_SerIdSeq).? ~
    (NewLine | End)
    ).map( d => NotamID(d._1,d._2,d._3) )
  
  // -----------------------------------------------------------------------------------------------------------
  case class NotamSeq(ser:String,id:Int,seq:Int) 
  case class NotamID(seq1:NotamSeq,typ:String,seq2:Option[NotamSeq]) {
    def describe:String = typ match {      
      case "N" => s"New: ${seq1.ser}:${seq1.id}/${seq1.seq}"
      case "C" => s"Cancel: ${seq1.ser}:${seq1.id}/${seq1.seq}"
      case "R" => s"Replace: ${seq1.ser}:${seq1.id}/${seq1.seq}: with ${seq2.get.ser}:${seq2.get.id}/${seq2.get.seq}"
    }
  }

  abstract class NotamData {
    def describe:String
  }

  case class NOTAM_Q(data:String) extends NotamData {
    def describe = s"Synopsis: '${data}'"
  }
  case class NOTAM_A(firs:Seq[String], extra:Option[String]) extends NotamData {
    def describe = s"ICAO: '${firs}' (${extra})"
  }
  case class NOTAM_B(date:ZonedDateTime) extends NotamData {
    def describe = s"Time Start: '${date}'"
  }
  case class NOTAM_C(date:ZonedDateTime) extends NotamData {
    def describe = s"Time End: '${date}'"
  }
  case class NOTAM_D(data:String) extends NotamData {
    def describe = s"Time Active: '${data}'"
  }
  case class NOTAM_E(data:String) extends NotamData {
    def describe = s"Desription: '${data}'"
  }
  case class NOTAM_F(alt:Altitude) extends NotamData {
    def describe = s"Lower Altitude Limit: '${alt}'"
  }
  case class NOTAM_G(alt:Altitude) extends NotamData {
    def describe = s"Upper Altitude Limit: '${alt}'"
  }

  case class NOTAM(
    id: Option[NotamID] = None,
    Q: Option[NOTAM_Q] = None,
    A: Option[NOTAM_A] = None,
    B: Option[NOTAM_B] = None,
    C: Option[NOTAM_C] = None,
    D: Option[NOTAM_D] = None,
    E: Option[NOTAM_E] = None,
    F: Option[NOTAM_F] = None,
    G: Option[NOTAM_G] = None,
  )

  def notamParser[_: P] = P( 
    line_1.? ~
    line_Q.? ~
    line_A.? ~
    line_B.? ~
    line_C.? ~
    line_D.? ~
    line_E.? ~
    line_F.? ~
    line_G.? ~
    End)
    .map { case(id,synopsis,location,startDate,endDate,active,explain,from,until) => {
      NOTAM(
        id,
        synopsis.map(d => NOTAM_Q(d)),
        location.map(d => NOTAM_A(d._1,d._2)),
        startDate.map(d => NOTAM_B(d)),
        endDate.map(d => NOTAM_C(d)),

        active.map(d => NOTAM_D(d)),
        explain.map(d => NOTAM_E(d)),
        from.map(d => NOTAM_F(d)),
        until.map(d => NOTAM_G(d)),
      )
  }}

  // fix FAA format (multiple A),B),C) on the same line): 'A) FIR B) ... C) ...'  
  def clean(data:String) = {
    data
      .trim
      .replaceAll(" A\\)","\nA\\)")
      .replaceAll(" B\\)","\nB\\)")
      .replaceAll(" C\\)","\nC\\)")
  }
      
  def decode(data:String): Try[NOTAM] = {
    log.debug(s"data='${data}'")
    val data1 = clean(data)
    println(s"data1='${data1}")
    val notam = parse(data1, Notam.notamParser(_))
    notam match {
      case Parsed.Success(v, index) => 
        Success(v.asInstanceOf[NOTAM])
      case f:Parsed.Failure => Failure(new Exception(f.toString))
    }
  }
}


