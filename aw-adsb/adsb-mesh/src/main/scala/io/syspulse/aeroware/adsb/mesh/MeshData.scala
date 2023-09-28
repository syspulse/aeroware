package io.syspulse.aeroware.adsb.mesh

import io.syspulse.skel.util.Util

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.aeroware.adsb.mesh.payload.PayloadType
import io.syspulse.aeroware.adsb.core.adsb.Raw

import spray.json._
import DefaultJsonProtocol._ 
import io.syspulse.skel.Ingestable


case class MeshData(
  ts: Long,         // timestamp of the miner (not real message ts, which can be different from different miners)
  pt: PayloadType,  // type of the raw data
  data: Raw         // raw data
) extends Ingestable


object MeshData {
  implicit val jf_fanout = jsonFormat3(MeshData.apply _)  
}
