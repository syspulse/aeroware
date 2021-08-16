package io.syspulse.aeroware.adsb.core

import scala.util._

import scodec.bits._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class DecoderSpec extends WordSpec with Matchers with Testables {

  val msgErr1 = "1"
  val msgErr2 = "1ffffffffffffffffffffffffffff"
  val msgErr3 = "*5d504e4be78915;"

  val msg1 = "8D4840D6202CC371C32CE0576098"

  val msg2 = "80a184b2582724b0545b728aefc3"

  val msgIdent1 = "8D50809520353276CB1D6037592E"
  val msgIdent2 = "8D4840D6202CC371C32CE0576098"

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
  
    s"decode ${msg1} as ADSB_AircraftIdentification type" in {
      val a1 = Decoder.decode(msg1)
      a1.get.getClass should === (ADSB_AircraftIdentification(17,5,AircraftAddress("4840d6","",""),0,0,"",null).getClass)
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
      a1.toEither.left.get.getMessage should === (s"invalid size: ${msgErr1.size}")
    }
  
    s"fail to decode ${msgErr2}" in {
      val a1 = Decoder.decode(msgErr2)
      info(s"${a1}")
      a1.isFailure should === (true)
      info(s"${a1.toEither.left}")
      a1.toEither.left.get.getMessage should === (s"invalid size: ${msgErr2.size}")
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

    s"decode '001011 001100 001101 110001 110000 110010 110011 100000' to 'KLM1023'" in {
      val v = Decoder.decodeDataAsChars(Seq(bin"001011",bin"001100",bin"001101",bin"110001",bin"110000",bin"110010",bin"110011",bin"100000"))
      v should === ("KLM1023")
    }

    s"decode ${msgIdent1} as ADSB_AircraftIdentification(MSI6215)" in {
      val a1 = Decoder.decode(msgIdent1)
      a1 should === (Success(ADSB_AircraftIdentification(17,5,AircraftAddress("508095","Antonov An-140","UR-14005"),4,0,"MSI6215",msgIdent1)))
    }

    s"decode ${msgIdent2} as ADSB_AircraftIdentification(KLM1023)" in {
      val a1 = Decoder.decode(msgIdent2)
      a1 should === (Success(ADSB_AircraftIdentification(17,5,AircraftAddress("4840D6","Fokker 70","PH-KZD"),4,0,"KLM1023",msgIdent2)))
    }
  }  
}
