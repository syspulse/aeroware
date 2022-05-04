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
  def validate(t:T):Boolean
}

class ValidationEngineADSB extends ValidationEngine[MSG_MinerData] {
  implicit val log = Logger(s"${this.getClass().getSimpleName()}")

  val tsDistance = new java.util.TreeMap[Long,Double]().asScala
  
  def validate(m:MSG_MinerData):Boolean = {
    // verify signature
    val adsbData = m.adsbs
    val sigData = upickle.default.writeBinary(adsbData)
    val sig = Util.hex2(m.sig.r) + ":" + Util.hex2(m.sig.s)

    val pk = m.pk
    val v = Eth.verify(sigData,sig,pk) //wallet.mverify(List(sig),sigData,None,None)
    if(!v) {
      log.error(s"Invalid signature: ${Eth.address(pk)}: ${m.sig}")
      return false
    }else
      log.info(s"Verified: ${Eth.address(pk)}: ${m.sig}")
    
    true
  }
}

trait RewardEngine {
  val minReward:Double = 0.1
  val maxReward:Double = 3.0

  def calculate(a:MSG_MinerData):Double
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
        (maxReward, 0.0)

    val rewardTs = if(tsDiff < 0 ) (r1,r2) else (r2,r1)

    rewardTs
  }

  def calculate(a1:MSG_MinerData): Double = {
    maxReward
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
    val k = Util.hex2(pk)
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
