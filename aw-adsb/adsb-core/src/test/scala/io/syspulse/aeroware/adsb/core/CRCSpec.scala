package io.syspulse.aeroware.asdb.core

import scala.util._

import org.scalatest.{ Matchers, WordSpec }

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class CRCSpec extends WordSpec with Matchers {

  
  val msg1    = "8D4840D6202CC371C32CE0576098"
  val msgErr1 = "8D5840D6202CC371C32CE0576098"

  val msg2    = "8d89653eea4c4858013f8c6472e1"
  val msgErr2 = "8d89653eea4c485c413f8c6472e1"
  
  "CRC" should {
    s"OK for ${msg1}" in {
      val crc1 = CRC.calc(msg1)
      info(s"${crc1}")

    }
  
    s"FAIL for ${msgErr1}" in {
      val crc1 = CRC.calc(msgErr1)
      info(s"${crc1}")

    }

    s"OK for ${msg2}" in {
      val crc1 = CRC.calc(msg2)
      info(s"${crc1}")

    }

    s"FAIL for ${msgErr2}" in {
      val crc1 = CRC.calc(msgErr2)
      info(s"${crc1}")

    }
  }
  
  
}
