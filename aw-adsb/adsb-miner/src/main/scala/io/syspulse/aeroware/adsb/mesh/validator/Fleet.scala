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

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.mesh.protocol._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.crypto.key.PK


trait ValidationEngine[T] {
  // account for possible reward adjustment (to prevent Spam)
  def validate(t:T):Double
}

class ValidationEngineADSB extends ValidationEngine[MSG_MinerData] {
  implicit val log = Logger(s"${this.getClass().getSimpleName()}")

  val tsDistance = new java.util.TreeMap[Long,Double]().asScala
  
  def validate(m:MSG_MinerData):Double = {
    // verify signature
    val adsbData = m.adsbs
    val pk = m.pk
    val addr = Eth.address(pk)
    
    if(m.adsbs.size == 0) {
      log.warn(s"No ADSB data: ${addr}")
      return RewardEngineADSB.penaltyNoData
    }

    val invalidCount = m.adsbs.filter( a => a.adsb == null || a.adsb.isBlank()).size
    if(invalidCount > 0) 
    {
      log.warn(s"Missing ADSB raw data: ${addr}")
      return RewardEngineADSB.penaltyMissingSomeData
    }

    val sigData = upickle.default.writeBinary(adsbData)
    val sig = Util.hex(m.sig.r) + ":" + Util.hex(m.sig.s)
    
    val v = Eth.verify(sigData,sig,pk) //wallet.mverify(List(sig),sigData,None,None)
    if(!v) {
      log.error(s"Invalid signature: ${addr}: ${m.sig}")
      return RewardEngineADSB.penaltyInvalidSig
    }else
      log.info(s"Verified: ${addr}: ${m.sig}")
    
    // validation does not mean reward
    return 0.0
  }
}

trait RewardEngine {
  val rewardMax:Double = 0.1
  val rewardMin:Double = 3.0

  def calculate(a:MSG_MinerData):Double
}

object RewardEngineADSB {
  val penaltyInvalidSig = -0.001
  val penaltyNoData = -0.002            // no ADSB Data
  val penaltyMissingSomeData = -0.0005  // some ADSB data
}

class RewardEngineADSB extends RewardEngine {

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

class MinerRewards(reward0:Double = 0.0) {
  // not thread safe, use Atomic or queue
  var reward:Double = reward0

  def get():Double= reward
  def +(r:Double) = reward = reward + r

  override def toString() = s"MinerRewards(${reward})"
}

class PeerMiner(pk:PK, tsRegister:Long = System.currentTimeMillis()) {
  val addr = Eth.address(pk)
  val rewards = new MinerRewards()
  def getAddr() = addr

  override def toString() = s"PeerMiner(${addr},${tsRegister},${rewards})"
}

class Fleet(config:Config) {
  val log = Logger(s"${this.getClass().getSimpleName()}")
  
  val miners: concurrent.Map[String, PeerMiner] = new ConcurrentHashMap().asScala
      
  def +(pk:PK): PeerMiner = {
    val k = Util.hex(pk)
    val miner = miners.get(k)
    if(! miner.isDefined) {
      val miner = new PeerMiner(pk)
      miners.putIfAbsent(k,miner).getOrElse(miner)
    } else 
      miner.get
  }

  override def toString() = {
    val ss = miners.values.map(m => s"${m.addr}: ${m.rewards.get()}").mkString("\n")
s"""=================================================
${ss}
=================================================
"""
  }

}
