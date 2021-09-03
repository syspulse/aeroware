package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._


class PipeStdout extends PipePositionBaro {
  
  def process(a0:ADSB_AirbornePositionBaro,a1:ADSB_AirbornePositionBaro) = {
    val loc = Decoder.getGloballPosition(a0,a1)
    if((a1.ts - aLast.get.ts) > 1L) {
      println(s"PositionBaro: ${loc.lat},${loc.lon},${loc.alt.alt},${a1.ts}")
      aLast = Some(a1)
    }
  }
}