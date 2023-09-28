package io.syspulse.aeroware.adsb.mesh.store

import io.syspulse.skel.util.Util

import spray.json._
import DefaultJsonProtocol._ 

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

import io.syspulse.skel.Ingestable
import io.syspulse.aeroware.adsb.mesh.payload.PayloadType

// already validated and signature verified
// stored into partition for further rewards calculation and feeding to extenal services
case class RawData(
  ts:Long,        // timestamp of when received to Validator
  addr:String,    // addr of miner
  ts0:Long,       // timestamp of how miner reported it
  pt:PayloadType,
  data:Raw,        // raw data
  penalty:Double
) extends Ingestable {
}

object RawData {  
  implicit val jf_v_data = jsonFormat6(RawData.apply _)
}
