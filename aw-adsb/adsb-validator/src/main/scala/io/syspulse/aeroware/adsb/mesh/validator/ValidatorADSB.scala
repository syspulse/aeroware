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
import io.syspulse.aeroware.adsb.mesh.guard.GuardEngine
import io.syspulse.aeroware.adsb.mesh.guard.GuardBlacklistAddr

class ValidatorADSB(ops:ValidatorConfig) extends ValidatorEngine[MSG_MinerData] {
    
  val guard = new GuardEngine(List()
    ++ { if(ops.validateAddrBlacklist) Seq(GuardBlacklistAddr(ops.blacklistAddr)) else Seq() }
    //++ { if(ops.validateIpBlacklist) Seq(GuardBlacklistIp(ops.blacklistIp)) else Seq() }
  )

  def validate(m:MSG_MinerData):Double = {
    // verify signature
    val data = m.data
    val addr = Util.hex(m.addr)

    val p = guard.permit(m)
    if(! p) {
      log.warn(s"Not permitted: ${addr}")
      return RewardADSB.penaltyNotPermitted
    }
    
    // check data is present 
    if(ops.validatePayload) {
      if(data.size == 0) {
        log.warn(s"Missing data: ${addr}")
        return RewardADSB.penaltyNoData
      }

      val invalidCount = data.filter( a => a.adsb == null || a.adsb.isBlank()).size
      
      if(invalidCount > 0) {
        log.warn(s"Missing ADSB raw data: ${addr}")
        return RewardADSB.penaltyMissingSomeData
      }
    }

    // check ts is not far close
    if(ops.validateTs) {
      val diff = Math.abs(System.currentTimeMillis - m.ts)
      if(diff > ops.toleranceTs) {
        log.warn(s"Timestamp diff above tolerance (${ops.toleranceTs}): ${diff}")
        return RewardADSB.penaltyTimeDiff
      }
    }

    if(ops.validateSig) {
      val sigData = upickle.default.writeBinary(data)
      val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)
      
      val v = Eth.verifyAddress(sigData,sig,addr)
      if(!v) {
        log.warn(s"Signature invalid: ${addr}: ${m.sig}")
        return RewardADSB.penaltyInvalidSig
      }
    }

    // validate the data is in ADSB format
    if(ops.validateData) {
      val penalty = 0.0
      m.data.foldLeft(0.0)( (r,d) => {
        val a = Decoder.decode(d.adsb,d.ts)
        a match {
          case Success(a) => 0.0
          case Failure(e) => RewardADSB.penaltyInvalidData
        }        
      }) 
    }
    
    // no penalties
    return 0.0
  }
}
