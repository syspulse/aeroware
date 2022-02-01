package io.syspulse.aeroware.adsb.util

import scala.util.{Try,Success,Failure}

import scodec.codecs._
import scodec.bits._
import scodec.Codec


import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

object Dump1090 {

  def decode(data:String) = {
    data.split("[\\*;]").filter(_.trim.size>0).head
  }
}
