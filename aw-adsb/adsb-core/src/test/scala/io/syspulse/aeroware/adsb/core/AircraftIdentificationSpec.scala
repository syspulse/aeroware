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

class AircraftIdentificationSpec extends AnyWordSpec with Matchers with Testables {

  val msgErr1 = "1"
  val msgErr2 = "1ffffffffffffffffffffffffffff"
  val msgErr3 = "*5d504e4be78915;"

  val msg1 = "8D4840D6202CC371C32CE0576098"

  val msg2 = "80a184b2582724b0545b728aefc3"

  val msgIdent1 = "8D50809520353276CB1D6037592E"
  val msgIdent2 = "8D4840D6202CC371C32CE0576098"

  "AircraftIdentificationSpec" should {
    
    s"decode ${msg1} as ADSB_AircraftIdentification type" in {
      val a1 = Adsb.decode(msg1)
      a1.get.getClass should === (ADSB_AircraftIdentification(17,5,AircraftAddress("4840d6","",""),0,0,"",null).getClass)
    }

    s"decode '001011 001100 001101 110001 110000 110010 110011 100000' to 'KLM1023'" in {
      val v = Adsb.decodeDataAsChars(Seq(bin"001011",bin"001100",bin"001101",bin"110001",bin"110000",bin"110010",bin"110011",bin"100000"))
      v should === ("KLM1023")
    }

    s"decode ${msgIdent1} as ADSB_AircraftIdentification(MSI6215)" in {
      val ts = System.currentTimeMillis
      val a1 = Adsb.decode(msgIdent1,ts)
      a1 should === (Success(ADSB_AircraftIdentification(17,5,AircraftAddress("508095","Antonov An-140","UR-14005"),4,0,"MSI6215",msgIdent1,ts)))
    }

    s"decode ${msgIdent2} as ADSB_AircraftIdentification(KLM1023)" in {
      val ts = System.currentTimeMillis
      val a1 = Adsb.decode(msgIdent2,ts)
      a1 should === (Success(ADSB_AircraftIdentification(17,5,AircraftAddress("4840D6","Fokker 70","PH-KZD"),4,0,"KLM1023",msgIdent2,ts)))
    }
  }  
}
