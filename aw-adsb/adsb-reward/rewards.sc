
import org.apache.spark.sql.functions.desc
import spark.implicits._
import org.apache.spark.sql.functions._

val df = spark.read.format("csv").option("header", "false").option("inferSchema", "true").load("output-2.csv").toDF("ts","addr","ts0","penalty","pt","data")

df.printSchema

val rewardData = 100.0

val dfPenalty = df.filter("penalty < 0.0")
val dfReward = df.filter("penalty >= 0.0")

// df.groupBy("addr").agg(sum("penalty"),count("penalty")).show

// penalty total
// val rddPenaltyTotal = dfPenalty.rdd.groupBy(r => r.getString(1)).map{ case(k,vv) => (k,vv.map(r => r.getDouble(3)).reduce(_ + _))}
val dfPenaltyTotal = dfPenalty.groupBy("addr").agg(sum("penalty").alias("penalty"))

// reward equally split between duplicate data miners
// val rddRewardTotal = dfReward.rdd.groupBy(r => r.getString(5)).flatMap(v => {val r = rewardData / v._2.size; v._2.map(m => (m.getString(1),r))}).groupBy(v => v._1).map{ case(k,vv) => (k,vv.map(_._2).reduce(_ + _))}
val dfRewardTotal = dfReward.groupBy("data").agg(count("penalty").alias("count")).withColumn("reward",lit(100.0) / col("count")).join(dfReward,"data").groupBy("addr").agg(sum("reward").alias("reward"))

// Expensive due to converation to DF !
// val dfPenaltyReward = rddRewardTotal.toDF.withColumnRenamed("_1","addr").withColumnRenamed("_2","reward").join(rddPenaltyTotal.toDF.withColumnRenamed("_1","addr"),Seq("addr"),"full").withColumnRenamed("_2","penalty").na.fill(0.0,Seq("penalty"))
val dfPenaltyReward = dfRewardTotal.join(dfPenaltyTotal,Seq("addr"),"full").na.fill(0.0,Seq("penalty"))

val dfTotal = dfPenaltyReward.withColumn("payout",col("reward") + col("penalty"))


// get duplicate data groups
// val df2 = df.groupBy("data").count().where("count > 1").drop("count").select("data")
// // join
// val df3 = df.join(df2,Seq("data"),"inner").sort("data")

// // work as RDD
// df3.rdd.groupBy(r => r.getString(0)).flatMap(v => {val r = rewardData / v._2.size; v._2.map(m => (m.getString(2),r))}).collect

// // aggregate total rewards
// df3.rdd.groupBy(r => r.getString(0)).flatMap(v => {val r = rewardData / v._2.size; v._2.map(m => (m.getString(2),r))}).groupBy(v => v._1).map{ case(k,vv) => (k,vv.map(_._2).reduce(_ + _))}.collect