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

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.mesh.protocol._
import scala.concurrent.ExecutionContext

class MinerReward(reward0:Double = 0.0) {
  // not thread safe, use Atomic or queue
  var reward:Double = reward0

  def get():Double= reward
  def +(r:Double) = reward = reward + r

  override def toString() = s"MinerReward(${reward})"
}

// class PeerMiner(pk:PK, tsRegister:Long = System.currentTimeMillis()) {
//   val addr = Eth.address(pk)
//   val rewards = new MinerRewards()
//   def getAddr() = addr

//   override def toString() = s"PeerMiner(${addr},${tsRegister},${rewards})"
// }

// class Miners() {
//   val log = Logger(s"${this.getClass().getSimpleName()}")
  
//   val miners: concurrent.Map[String, PeerMiner] = new ConcurrentHashMap().asScala
      
//   def +(msg:MSG_MinerData,reward:Double = 0.0): PeerMiner = {

//     val pk:PK = msg.pk
//     val pkStr = Util.hex(pk)

//     val miner = miners.get(pkStr)
//     if(! miner.isDefined) {
//       val miner = new PeerMiner(pk)
//       miners.putIfAbsent(pkStr,miner).getOrElse(miner)
//     } else 
//       miner.get
//   }

//   override def toString() = {
//     val ss = miners.values.map(m => s"${m.addr}: ${m.rewards.get()}").mkString("\n")
// s"""=================================================
// ${ss}
// =================================================
// """
//   }
// 
// }
