package io.syspulse.aeroware.adsb.miner

import io.syspulse.skel.util.Util

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw
import com.fasterxml.jackson.module.scala.deser.overrides

case class MSG_MinerData(ts: Long, addr:Array[Byte], adsbs: Array[MSG_MinerADSB], sig:Array[Byte]) extends MSG_Miner {
  override def toString = s"${this.getClass.getSimpleName}(${ts},${Util.hex2(addr)},${adsbs.toSeq},${Util.hex2(sig)})"
}

object MSG_MinerData {
  implicit val rw: RW[MSG_MinerData] = macroRW
}
