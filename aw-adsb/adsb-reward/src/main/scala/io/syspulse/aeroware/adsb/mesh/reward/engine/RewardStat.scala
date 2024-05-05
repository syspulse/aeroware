package io.syspulse.aeroware.adsb.mesh.reward.engine

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection

import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

//import scala.collection.mutable.TreeMap
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors


case class RewardMiner(
  addr:String,  // address of the miner
  reward:Double,
  rewardNum:Long,
  penalty:Double,
  penaltyNum:Long,
  payout:Double)

case class RewardStat(
  miners:Seq[RewardMiner] = Seq()
)

object RewardMiner {
  def apply(addr:String) = new RewardMiner(addr,0.0, 0L, 0.0, 0L, 0.0) 
}
