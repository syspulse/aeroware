package io.syspulse.aeroware.adsb.mesh.validator

import scala.util.{Try,Failure,Success}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger
import scala.util.Random

import scala.collection.concurrent
import scala.jdk.CollectionConverters._

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles
import io.syspulse.skel.crypto.key.PK

import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.rewards._
import io.syspulse.aeroware.notam.Notam

class ValidatorNOTAM(ops:ValidatorConfig) extends ValidatorCore(ops) {
  override def validate(m:MSG_MinerData):Double = {
    var p0 = super.validate(m)
    
    // validate the data is in ADSB format
    val p1 = if(ops.validateData) {
      m.payload.foldLeft(0.0)( (r,d) => {
        val a = Notam.decode(d.data)
        a match {
          case Success(a) => 0.0
          case Failure(e) => 
            log.warn(s"could not decode: ${d.data}",e)
            Rewards.penaltyInvalidData
        }        
      }) 
    } else 0.0

    return p0 + p1
  }
}

