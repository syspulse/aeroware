package io.syspulse.aeroware.adsb.mesh.rewards

import scala.util.{Try,Failure,Success}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger
import scala.util.Random

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters._

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.mesh.protocol._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.crypto.key.PK

import io.syspulse.aeroware.adsb.mesh.store.RawStore

class RewardADSB extends RewardEngine {

  // distance between two competing ADSB messages
  def between(a1:MSG_MinerData,a2:MSG_MinerData):(Double,Double) = {
    val tsDiff = a1.ts - a2.ts
    val (r1,r2) = 
      if(tsDiff == 0L) (0.25, 0.25)
      else
      if(math.abs(tsDiff) < 10) (1.0, 0.5)
      else
      if(math.abs(tsDiff) < 50) (2.0, 0.1)
      else
        (rewardMax, 0.0)

    val rewardTs = if(tsDiff < 0 ) (r1,r2) else (r2,r1)

    rewardTs
  }

  def calculate(a1:MSG_MinerData): Double = {
    rewardMax
  }
}
