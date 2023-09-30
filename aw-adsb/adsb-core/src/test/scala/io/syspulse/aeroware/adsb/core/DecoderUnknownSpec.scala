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

class DecoderUnknownSpec extends AnyWordSpec with Matchers with Testables {

  val msgUnknown1 = "5DA606933B451A"

  // there are NewLines !
  val msgWar1 = """*00000000000000;
*00000000000000;
"""
  // there are NewLines !
  val msgWar2 = """*A800080010030A80F500006E9FAD;
*A80008002009224FDB88201E362A;
"""
  
  "DecoderUnknown" should {    
    s"decode dump1090 message: ${msgUnknown1} as type 11" in {
      val a1 = Adsb.decode(msgUnknown1)
      // info(s"${a1}")
      a1.isFailure should === (false)
      a1.get.df should === (11)
      a1.get.capability should === (5)
    }

    s"decode message: A800080010030A80F500006E9FAD" in {
      val a1 = Adsb.decode("A800080010030A80F500006E9FAD")
      info(s"${a1}")
      a1.isFailure should === (false)
      a1.get.df should === (21)
      a1.get.capability should === (0)
    }
  }

}
