package io.syspulse.aeroware.adsb.mesh.store

import io.syspulse.skel.util.Util

import spray.json._
import DefaultJsonProtocol._ 

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

import io.syspulse.skel.Ingestable

// already validated and signature verified
// stored into partition for further rewards calculation and feeding to extenal services
case class ValidatorData(
  ts:Long,        // timestamp of when received to Validator
  addr:String,    // addr of miner
  ts0:Long,       // timestamp of how miner reported it
  data:Raw        // raw data
) extends Ingestable {
}

object ValidatorData {  
  implicit val jf_v_data = jsonFormat4(ValidatorData.apply _)
}
