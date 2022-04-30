package io.syspulse.aeroware.adsb.mesh.protocol

import io.syspulse.skel.util.Util

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw
import com.fasterxml.jackson.module.scala.deser.overrides

case class MinerSig(r: Array[Byte], s: Array[Byte]) {
  override def toString = s"${this.getClass.getSimpleName}(${Util.hex2(r)}:${Util.hex2(s)})"
}

object MinerSig {
  implicit val rw: RW[MinerSig] = macroRW

  def apply(sig:String):MinerSig = {
    sig.split(":") match {
      case Array(r,s) => new MinerSig(r = Util.fromHexString(r),s = Util.fromHexString(s))
      case Array(r) => new MinerSig(r = Util.fromHexString(r),s = Array())
      case _ => new MinerSig(r = Array(),s = Array())
    }
  }
}

case class MSG_MinerData(ts: Long, addr:Array[Byte], adsbs: Array[MSG_MinerADSB], sigData:Array[Byte],sig:MinerSig,ver:Int = MSG_Version.V1) extends MSG_Miner {
  override def toString = s"${this.getClass.getSimpleName}(0x${ver.toHexString},${ts},${Util.hex2(addr)},${adsbs.toSeq},${sig})"
}

object MSG_MinerData {
  implicit val rw: RW[MSG_MinerData] = macroRW
}
