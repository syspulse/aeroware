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
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.mesh.protocol._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.crypto.key.PK

import io.syspulse.aeroware.adsb.mesh.store.DataStore

case class RewardMiner(addr:String,messages:Long,reward:Double)

case class RewardResults(count:Long,totalReward:Double,miners:Set[RewardMiner])

trait RewardEngine {
  val rewardMax:Double = 0.1
  val rewardMin:Double = 3.0

  def calculate(a:MSG_MinerData):Double

  def calculate(ts0:Long,ts1:Long,store:DataStore):RewardResults = {
    RewardResults(0,0.0,Set()) 
  }

}
