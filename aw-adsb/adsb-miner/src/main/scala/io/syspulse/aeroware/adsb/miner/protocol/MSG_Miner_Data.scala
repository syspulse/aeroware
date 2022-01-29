package io.syspulse.aeroware.adsb.miner

import io.syspulse.skel.util.Util

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw
import com.fasterxml.jackson.module.scala.deser.overrides

case class MSG_Miner_Data(ts: Long, addr:Array[Byte], adsbs: Array[MSG_Miner_ADSB], sig:Array[Byte]) extends MSG_Miner {
  override def toString = s"${this.getClass.getSimpleName}(${ts},${Util.hex2(addr)},${adsbs.toSeq},${Util.hex2(sig)})"
}

object MSG_Miner_Data {
  implicit val rw: RW[MSG_Miner_Data] = macroRW
}
