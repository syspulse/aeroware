package io.syspulse.aeroware.asdb.core

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class DecoderSpec extends WordSpec with Matchers {

  val msgErr1 = "1"
  val msgErr2 = "1ffffffffffffffffffffffffffff"

  val msg1 = "8D4840D6202CC371C32CE0576098"
  val file1 = "test-1.adsb"

  def load(file:String) = {
    val txt = scala.io.Source.fromResource(file).getLines()
    txt.toSeq.filter(_.trim.size>0).map( s => s.split("[\\*;]").filter(_.trim.size>0)).flatten
  }

  "Decoder" should {
    s"decode ${msg1} DF=17" in {
      val a1 = Decoder.decode(msg1)
      info(s"${a1}")
      a1.get.df should === (17)
    }
  }

  "Decoder" should {
    s"decode ${msg1} Capability=5" in {
      val a1 = Decoder.decode(msg1)
      info(s"${a1}")
      a1.get.capability should === (5)
    }
  }

  "Decoder" should {
    s"decode ${msg1} ICAO Aircraft Address" in {
      val a1 = Decoder.decode(msg1)
      info(s"${a1}")
      a1.get.aircraftAddr should === (AircraftAddress("4840d6","Fokker 70","PH-KZD"))
    }
  }

  "Decoder" should {
    s"decode ${msg1} as ADSB_AircraftIdentification" in {
      val a1 = Decoder.decode(msg1)
      info(s"${a1}")
      a1.get.getClass should === (ADSB_AircraftIdentification(17,5,AircraftAddress("4840d6","Fokker 70","PH-KZD"),null).getClass)
    }
  }

  "Decoder" should {
    s"decode all ${file1} without crash" in {
      load(file1).map( message => {
        val a1 = Decoder.decode(message)
        info(s"${message} -> ${a1}")
        //a1.getClass should === (ADSB_AircraftIdentification(17,5,AircraftAddress("4840d6","Fokker 70","PH-KZD"),null).getClass)
      })
    }
  }

  "Decoder" should {
    s"fail to decode ${msgErr1}" in {
      val a1 = Decoder.decode(msgErr1)
      info(s"${a1}")
      a1.isFailure should === (true)
      info(s"${a1.toEither.left}")
      a1.toEither.left.get.getMessage should === (s"invalid size: ${msgErr1.size}")
    }
  }

  "Decoder" should {
    s"fail to decode ${msgErr2}" in {
      val a1 = Decoder.decode(msgErr2)
      info(s"${a1}")
      a1.isFailure should === (true)
      info(s"${a1.toEither.left}")
      a1.toEither.left.get.getMessage should === (s"invalid size: ${msgErr2.size}")
    }
  }

  
}
