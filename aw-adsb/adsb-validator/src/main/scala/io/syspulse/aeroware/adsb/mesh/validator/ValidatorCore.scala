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
import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.rewards._
import io.syspulse.aeroware.adsb.mesh.guard.GuardEngine
import io.syspulse.aeroware.adsb.mesh.guard.GuardBlacklistAddr
import io.syspulse.aeroware.adsb.mesh.guard.GuardBlacklistIp

abstract class ValidatorCore(ops:ValidatorConfig) extends ValidatorPenalty[MSG_MinerData] {
    
  val guard = new GuardEngine(List()
    ++ { if(ops.validateAddrBlacklist) Seq(GuardBlacklistAddr(ops.blacklistAddr)) else Seq() }
    ++ { if(ops.validateIpBlacklist) Seq(GuardBlacklistIp(ops.blacklistIp)) else Seq() }
  )

  // ATTENTION: penalties are not accumulative 
  def validate(m:MSG_MinerData):Double = {
    // verify signature
    val data = m.payload
    val addr = Util.hex(m.addr)

    val p = guard.permit(m)
    if(! p) {
      log.warn(s"Not permitted: ${addr}")
      return Rewards.penaltyNotPermitted
    }
    
    // check data is present 
    if(ops.validatePayload) {
      if(data.size == 0) {
        log.warn(s"Missing data: ${addr}")
        return Rewards.penaltyNoData
      }

      val invalidCount = data.filter( a => a.data == null || a.data.isBlank()).size
      
      if(invalidCount > 0) {
        log.warn(s"Missing raw data: ${addr}")
        return Rewards.penaltyMissingSomeData
      }
    }

    // check ts is not far into the past
    if(ops.validateTs) {
      val diff = Math.abs(System.currentTimeMillis - m.ts)
      if(diff > ops.toleranceTs) {
        log.warn(s"Timestamp diff above tolerance (${ops.toleranceTs}): ${diff}")
        return Rewards.penaltyTimeDiff
      }
    }

    if(ops.validateSig) {
      val sigData = upickle.default.writeBinary(data)
      val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)
      
      val v = Eth.verifyAddress(sigData,sig,addr)
      if(!v) {
        log.warn(s"Signature invalid: ${addr}: ${m.sig}")
        return Rewards.penaltyInvalidSig
      }
    }
    
    // no penalties
    return 0.0
  }
}
