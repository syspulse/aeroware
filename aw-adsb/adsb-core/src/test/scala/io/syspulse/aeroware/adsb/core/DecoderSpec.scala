package io.syspulse.aeroware.adsb.core

import scala.util._

import scodec.bits._

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import java.time._
import io.jvm.uuid._

import io.syspulse.aeroware.adsb.Testables
import io.syspulse.skel.util.Util

class DecoderSpec extends AnyWordSpec with Matchers with Testables {

  val msgErr1 = "1"
  val msgErr2 = "1ffffffffffffffffffffffffffff"
  val msgErr3 = "*5d504e4be78915;"

  val msg1 = "8D4840D6202CC371C32CE0576098"

  val msg2 = "80a184b2582724b0545b728aefc3"

  val msgIdent1 = "8D50809520353276CB1D6037592E"
  val msgIdent2 = "8D4840D6202CC371C32CE0576098"

  val msgUnknown1 = "5DA606933B451A"

  val msgWar1 = "*00000000000000;"
  val msgWar2 = """*A800080010030A80F500006E9FAD;
*A80008002009224FDB88201E362A;"""

  "Decoder" should {
    s"decode ${msg1} DF=17" in {
      val a1 = Decoder.decode(msg1)
      a1.get.df should === (17)
    }
  
    s"decode ${msg1} Capability=5" in {
      val a1 = Decoder.decode(msg1)
      a1.get.capability should === (5)
    }
  
    s"decode ${msg1} ICAO Aircraft Address" in {
      val a1 = Decoder.decode(msg1)
      a1.get.aircraftAddr should === (AircraftAddress("4840d6","Fokker 70","PH-KZD"))
    }
  
    s"decode all ${file1} without crash" in {
      load(file1).map( message => {
        val a1 = Decoder.decode(message)
        info(s"${message} -> ${a1}")
        //a1.getClass should === (ADSB_AircraftIdentification(17,5,AircraftAddress("4840d6","Fokker 70","PH-KZD"),null).getClass)
      })
    }
  
    s"fail to decode ${msgErr1}" in {
      val a1 = Decoder.decode(msgErr1)
      info(s"${a1}")
      a1.isFailure should === (true)
      info(s"${a1.toEither.left}")
      a1.toEither.left.get.getMessage should === (s"invalid size: ${msgErr1.size}: ${msgErr1}")
    }
  
    s"fail to decode ${msgErr2}" in {
      val a1 = Decoder.decode(msgErr2)
      info(s"${a1}")
      a1.isFailure should === (true)
      info(s"${a1.toEither.left}")
      a1.toEither.left.get.getMessage should === (s"invalid size: ${msgErr2.size}: ${msgErr2}")
    }
  
    s"fail to decode dump1090 format message: ${msgErr3}" in {
      val a1 = Decoder.decode(msgErr3)
      info(s"${a1}")
      a1.isFailure should === (true)
      info(s"${a1.toEither.left}")
      a1.toEither.left.get.getMessage should === (s"invalid format: failed to parse DF: ${msgErr3}")
    }

    s"decode dump1090 format message with decodeDump1090: ${msgErr3}" in {
      val a1 = Decoder.decodeDump1090(msgErr3)
      info(s"${a1}")
      a1.isSuccess should === (true)
    }

    s"decode LocalACAS to ADSB_Unknown: ${msg2}" in {
      val a1 = Decoder.decode(msg2)
      info(s"${a1}")
      a1.isFailure should === (false)
      a1.get.getClass.getSimpleName should === ("ADSB_Unknown")
    }

    s"decode bin'000001' to 'A'" in {
      val v = Decoder.decodeCharacter(bin"000001")
      v should === ('A')
    }

    s"decode 26 to 'Z'" in {
      val v = Decoder.decodeCharacter(BitVector.fromBin(26.toBinaryString).get)
      v should === ('Z')
    }

    s"decode 48 to '0'" in {
      val v = Decoder.decodeCharacter(BitVector.fromBin(48.toBinaryString).get)
      v should === ('0')
    }

    s"decode 57 to '9'" in {
      val v = Decoder.decodeCharacter(BitVector.fromBin(57.toBinaryString).get)
      v should === ('9')
    }

    s"decode 32 to ' '" in {
      val v = Decoder.decodeCharacter(BitVector.fromBin(32.toBinaryString).get)
      v should === (' ')
    }

    s"decode 30 (invalid) to '#'" in {
      val v = Decoder.decodeCharacter(BitVector.fromBin(30.toBinaryString).get)
      v should === ('#')
    }

  }  

  "DecoderUnknown" should {    
    s"decode dump1090 message: ${msgUnknown1} as type 11" in {
      val a1 = Decoder.decode(msgUnknown1)
      info(s"${a1}")
      a1.isFailure should === (false)      
    }
  }  
}
