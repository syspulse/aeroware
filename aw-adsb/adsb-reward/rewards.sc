
import org.apache.spark.sql.functions.desc
import spark.implicits._
import org.apache.spark.sql.functions._

val df = spark.read.format("csv").option("header", "false").option("inferSchema", "true").load("output.csv").toDF("ts","addr","ts0","penalty","pt","data")

df.printSchema

// df.groupBy("addr").agg(sum("penalty"),count("penalty")).show

// get duplicate data groups
val df2 = df.groupBy("data").count().where("count > 1").drop("count").select("data")
// join
val df3 = df.join(df2,Seq("data"),"inner").sort("data")

// work as RDD
df3.rdd.groupBy(r => r.getString(0)).flatMap(v => {val r = 100.0 / v._2.size; v._2.map(m => (m.getString(2),r))}).collect

// aggregate total rewards
df3.rdd.groupBy(r => r.getString(0)).flatMap(v => {val r = 100.0 / v._2.size; v._2.map(m => (m.getString(2),r))}).groupBy(v => v._1).map{ case(k,vv) => (k,vv.map(_._2).reduce(_ + _))}.collect