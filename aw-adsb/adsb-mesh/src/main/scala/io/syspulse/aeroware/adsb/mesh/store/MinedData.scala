package io.syspulse.aeroware.adsb.mesh.store

import io.syspulse.skel.util.Util

import spray.json._
import DefaultJsonProtocol._ 

import io.syspulse.aeroware.core.Raw
import io.syspulse.aeroware.adsb.core.ADSB

import io.syspulse.skel.Ingestable
import io.syspulse.aeroware.adsb.mesh.payload.PayloadType

import io.syspulse.aeroware.adsb.mesh.Hash

// already validated and signature verified
// stored into partition for further rewards calculation and feeding to extenal services
case class MinedData(
  ts:Long,        // timestamp of when received to Validator
  addr:String,    // addr of miner
  ts0:Long,       // timestamp of how miner reported it
  penalty:Double,
  pt:PayloadType,
  data:Hash,      // raw data hash. Raw data is saved separately
  size:Int,       // raw data size
  
) extends Ingestable {
}

object MinedData {  
  def apply(ts:Long,addr:String,ts0:Long,penalty:Double,pt:PayloadType,data:Raw) =
    new MinedData(ts,addr,ts0,penalty,pt,toHash(data),data.size)
    
  def toHash(data:Raw):Hash = Util.hex(Util.SHA256(data.getBytes()))
}

object MinedDataJson {  
  implicit val jf_v_data = jsonFormat6(MinedData.apply _)
}

