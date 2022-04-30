package io.syspulse.aeroware.adsb.mesh.protocol

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

case class MSG_MinerADSB(ts: Long, adsb: Raw)

object MSG_MinerADSB {
  implicit val rw: RW[MSG_MinerADSB] = macroRW
}


