package io.syspulse.aeroware.adsb.mesh.validation

import scala.util.{Try,Failure,Success}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger
import scala.util.Random

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles
import io.syspulse.skel.crypto.key.PK

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.rewards._

class ValidationADSB extends ValidationEngine[MSG_MinerData] {
  
  val tsDistance = new java.util.TreeMap[Long,Double]().asScala
  
  def validate(m:MSG_MinerData):Double = {
    // verify signature
    val adsbData = m.adsbs
    val pk = m.pk
    val addr = Eth.address(pk)
    
    if(m.adsbs.size == 0) {
      log.warn(s"No ADSB data: ${addr}")
      return RewardADSB.penaltyNoData
    }

    val invalidCount = m.adsbs.filter( a => a.adsb == null || a.adsb.isBlank()).size
    if(invalidCount > 0) 
    {
      log.warn(s"Missing ADSB raw data: ${addr}")
      return RewardADSB.penaltyMissingSomeData
    }

    val sigData = upickle.default.writeBinary(adsbData)
    val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)
    
    val v = Eth.verify(sigData,sig,pk)
    if(!v) {
      log.warn(s"Invalid signature: ${addr}: ${m.sig}")
      return RewardADSB.penaltyInvalidSig
    }else
      log.info(s"Verified: ${addr}: ${m.sig}")
    
    // validation does not mean reward
    return 0.0
  }
}
