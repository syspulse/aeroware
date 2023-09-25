package io.syspulse.aeroware.adsb.mesh.protocol

import io.syspulse.skel.util.Util

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

import spray.json._
import DefaultJsonProtocol._ 
import io.syspulse.skel.Ingestable

case class MeshData(
  ts: Long,       // timestamp of the miner (not ADSB ts, which can be different from different miners)
  data: Raw       // raw ADSB
) extends Ingestable


object MeshData {
  implicit val jf_fanout = jsonFormat2(MeshData.apply _)  
}
