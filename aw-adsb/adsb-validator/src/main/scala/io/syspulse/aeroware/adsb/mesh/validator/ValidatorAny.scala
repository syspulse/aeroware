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
import io.syspulse.aeroware.adsb.mesh.guard.GuardEngine
import io.syspulse.aeroware.adsb.mesh.guard.GuardBlacklistAddr
import io.syspulse.aeroware.adsb.mesh.guard.GuardBlacklistIp
import io.syspulse.aeroware.adsb.mesh.PayloadTypes
import io.syspulse.aeroware.notam.Notam
import io.syspulse.aeroware.metar.Metar
import io.syspulse.aeroware.adsb.mesh.rewards.Rewards

// can validate anything based on PayloadType
class ValidatorAny(ops:ValidatorConfig) extends ValidatorCore(ops) {
      
  override def validate(m:MSG_MinerData):Double = {
    var p0 = super.validate(m)
    
    // validate the data is in ADSB format
    val p1 = if(ops.validateData) {      
      m.payload.foldLeft(0.0)( (r,d) => {
        val a = d.pt match {
          case PayloadTypes.ADSB => Adsb.decode(d.data,d.ts)
          case PayloadTypes.NOTAM => Notam.decode(d.data)
          case PayloadTypes.METAR => Metar.decode(d.data)
          case _ =>
        }
        
        a match {
          case Success(a) => 0.0
          case Failure(e) => 
            log.warn(s"could not decode: ${d.data}",e)
            Rewards.penaltyInvalidData
        }   
      }) 
    } else 0.0
    
    // no penalties
    return p0 + p1
  }
}
