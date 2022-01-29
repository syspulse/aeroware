package io.syspulse.aeroware.adsb.miner

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

case class MSG_Miner_ADSB(ts: Long, adsb: Raw) extends MSG_Miner

object MSG_Miner_ADSB {
  implicit val rw: RW[MSG_Miner_ADSB] = macroRW
}


