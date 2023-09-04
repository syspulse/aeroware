package io.syspulse.aeroware.adsb.mesh.validator

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

class ValidatorADSB(validateTs:Boolean = false,toleranceTs:Long = 1000L) extends ValidatorEngine[MSG_MinerData] {
    
  def validate(m:MSG_MinerData):Double = {
    // verify signature
    val data = m.data
    val addr = Util.hex(m.addr)
    
    if(data.size == 0) {
      log.warn(s"No ADSB data: ${addr}")
      return RewardADSB.penaltyNoData
    }

    // check data present
    val invalidCount = data.filter( a => a.adsb == null || a.adsb.isBlank()).size
    if(invalidCount > 0) {
      log.warn(s"Missing ADSB raw data: ${addr}")
      return RewardADSB.penaltyMissingSomeData
    }

    // check ts is not far close
    if(validateTs) {
      val diff = Math.abs(System.currentTimeMillis - m.ts)
      if(diff > toleranceTs) {
        log.warn(s"Timestamp diff above tolerance (${toleranceTs}): ${diff}")
        return RewardADSB.penaltyMissingSomeData
      }
    }

    val sigData = upickle.default.writeBinary(data)
    val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)
    
    val v = Eth.verifyAddress(sigData,sig,addr)
    if(!v) {
      log.warn(s"Signature invalid: ${addr}: ${m.sig}")
      return RewardADSB.penaltyInvalidSig
    }
    
    // validation does not mean reward
    return 0.0
  }
}
