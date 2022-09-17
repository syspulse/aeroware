package io.syspulse.aeroware.adsb.mesh.protocol

import io.syspulse.skel.util.Util

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat, deserializationError}
import spray.json.RootJsonFormat
import spray.json.JsObject
import spray.json.JsNumber
import spray.json.JsArray
import spray.json._
import DefaultJsonProtocol._ 
import io.syspulse.skel.Ingestable


case class MinerSig(r: Array[Byte], s: Array[Byte]) {
  override def toString = s"${this.getClass.getSimpleName}(${Util.hex(r)}:${Util.hex(s)})"
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

case class MSG_MinerData(ts: Long, pk:Array[Byte], adsbs: Array[MSG_MinerADSB], sig:MinerSig,ops:Int = MSG_Options.V_1 | MSG_Options.O_EC,socket:String="") extends MSG_Miner with Ingestable {
  override def toString = s"${this.getClass.getSimpleName}(0x${ops.toHexString},${ts},${Util.hex(pk)},${adsbs.toSeq},${sig})"
}

object MSG_MinerData {
  implicit val rw: RW[MSG_MinerData] = macroRW

  implicit val jf_ms = jsonFormat2(MinerSig.apply _)
  implicit val jf_ma = jsonFormat2(MSG_MinerADSB.apply _)
  implicit val jf_md = jsonFormat6(MSG_MinerData.apply _)
  
}
