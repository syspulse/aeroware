package io.syspulse.aeroware.adsb.core

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.util._

class CRCSpec extends WordSpec with Matchers with Testables {

  
  val msg1    = "8D4840D6202CC371C32CE0576098"
  val msgErr1 = "8D5840D6202CC371C32CE0576098"

  val msg2    = "8d89653eea4c4858013f8c6472e1"
  val msgErr2 = "8d89653eea4c485c413f8c6472e1"
  
  "CRC" should {
    s"OK for ${msg1}" in {
      val crc1 = CRC.calc(msg1)
      crc1 should === (true)
    }
  
    s"FAIL for ${msgErr1}" in {
      val crc1 = CRC.calc(msgErr1)
      crc1 should === (false)
    }

    s"OK for ${msg2}" in {
      val crc1 = CRC.calc(msg2)
      crc1 should === (true)
    }

    s"FAIL for ${msgErr2}" in {
      val crc1 = CRC.calc(msgErr2)
      crc1 should === (false)
    }

    s"check CRC OK for all 8.... data from ${file1}" in {
      load(file1).filter(_.size == 28).filter(_.startsWith("8")).map( data => {
        val crc1 = CRC.calc(data)
        info(s"${data} -> ${crc1}")
        crc1 should === (true)
      })
    }
  }
  
  
}
