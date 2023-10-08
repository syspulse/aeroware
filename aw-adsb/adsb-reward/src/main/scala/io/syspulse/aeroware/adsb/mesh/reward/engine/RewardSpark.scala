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

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.mesh.reward.Config


import org.apache.spark.sql._
import org.apache.spark.sql.functions.desc
import org.apache.spark.sql.functions._
import io.syspulse.aeroware.adsb.mesh.rewards.Rewards
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types._

trait RewardEngine {
  def calculateRewards():Try[RewardStat]
}

class RewardSpark(dir:String = "./lake/")(implicit config:Config) extends RewardEngine {
  
  val log = Logger(s"${this}")
  
  def calculateRewards(): Try[RewardStat] = {
    val spark = SparkSession.builder()
      .appName("adsb-reward")
      .master("local[2]")
      .config("spark.executor.memory", "1g")
      .config("spark.driver.memory", "1g")
      .getOrCreate()

    log.info(s"Loading: '${dir}'")

    val schema0 = StructType(Array(
      StructField("ts",LongType,true),
      StructField("addr",StringType,true),
      StructField("ts0",LongType,true),
      StructField("penalty",DoubleType,true),
      StructField("pt",IntegerType,true),
      StructField("data",StringType,true)
    ))

    val df = spark
      .read
      .schema(schema0)
      .format(config.format)
      .option("recursiveFileLookup", "true")
      .option("header", "false")
      .option("inferSchema", "true")
      .load(dir + "*")      
      .toDF("ts","addr","ts0","penalty","pt","data")
    
    import spark.implicits._

    // log.info(s"schema: ${df.schema}")
    //df.printSchema()

    log.info(s"Calculating rewards: ${df.schema}...")

    val rewardData = Rewards.rewardData

    val dfPenalty = df.filter("penalty < 0.0")
    val dfReward = df.filter("penalty == 0.0")

    val dfPenaltyTotal = dfPenalty.groupBy("addr","pt").agg(sum("penalty").alias("penalty"),count("data").alias("num_penalty"))

    val dfRewardTotal = dfReward.groupBy("data","pt").agg(count("penalty").alias("count"),count("data").alias("num_reward")).withColumn("reward",lit(rewardData) / col("count")).join(dfReward,Seq("data","pt")).groupBy("addr","pt").agg(sum("reward").alias("reward"),sum("num_reward").alias("num_reward"))

    val dfPenaltyReward = dfRewardTotal.join(dfPenaltyTotal,Seq("addr","pt"),"full").na.fill(0.0,Seq("penalty")).na.fill(0,Seq("num_penalty"))

    // baseline without taking into account PayloadTypes
    val dfTotal = dfPenaltyReward.withColumn("payout",col("reward") + col("penalty"))

    log.info(s"Calculating payouts...")

    // reduce to group on different payload types.
    // this dataset should be MINERS * PAYLOAD_TYPES max size (<100K)
    val totalAddr = dfTotal.rdd.groupBy(r => r.getString(0)).collect().toMap

    log.info(s"Calculating payouts: total=${totalAddr.size}")
    
    val rewards = totalAddr.map{ 
      case(addr,rr) => rr.foldLeft(RewardMiner(addr))(
        (rm,r) => {
          val payloadType = r.getInt(1)
          val payoutCoeff = Rewards.rewardDataType.get(payloadType).getOrElse(1.0)

          rm.copy(
            reward = r.getDouble(2) * payoutCoeff,
            rewardNum = r.getLong(3),
            penalty = r.getDouble(4) * payoutCoeff,
            penaltyNum = r.getLong(5),
            payout = r.getDouble(6) * payoutCoeff
          )
        }
      )
    }

    val stat = RewardStat(rewards.toSeq)
    
    Success(stat)
  }

}