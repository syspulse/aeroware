package io.syspulse.aeroware.adsb.mesh.protocol

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.mesh.payload.PayloadType

case class MSG_MinerPayload(ts: Long, pt:PayloadType, data: Raw)

object MSG_MinerPayload {
  implicit val rw: RW[MSG_MinerPayload] = macroRW
}


