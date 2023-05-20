package io.syspulse.aeroware.adsb.mesh.validator

import com.typesafe.scalalogging.Logger

import java.time.Instant
import scala.concurrent.Awaitable
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Options
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

import io.syspulse.aeroware.adsb.mesh.rewards._
import io.syspulse.aeroware.adsb.mesh.store._


case class Config (
  feed:String = "",
  output:String = "",

  keystore:String = "",
  keystorePass:String = "",
  batchSize: Int = 3,
  batchWindow: Long = 1000L,
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,

  entity:String = "",
  format:String = "",
  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "",
  buffer:Int = 0,
  throttle:Long = 0L,

  filter:Seq[String] = Seq.empty,

  datastore:String = "mem",

  cmd:String = "",
  params: Seq[String] = Seq(),
)

object App extends skel.Server {
  import MSG_MinerData._

  def main(args: Array[String]):Unit = {
    Console.err.println(s"args: '${args.mkString(",")}'")

    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-validator","",
        
        ArgString('_', "keystore.file","Keystore file (def: ./keystore/)"),
        ArgString('_', "keystore.pass","Keystore password"),        
        ArgInt('_', "batch.size","ADSB Batch max size"),
        ArgLong('_', "batch.window","ADSB Batch time window (msec)"),
        ArgString('_', "proto.options",s"Protocol options (def: ${MSG_Options.defaultArg})"),
        
        ArgString('f', "feed","Input Feed (def: )"),
        ArgString('o', "output","Output file (pattern is supported: data-{yyyy-MM-dd-HH-mm}.log)"),
        ArgString('e', "entity","Ingest entity: (def: all)"),
        
        ArgString('_', "format","Outptu format (none,json,csv) (def: none)"),

        ArgLong('_', "limit","Limit"),
        ArgLong('_', "freq","Frequency"),
        ArgString('_', "delimiter","""Delimiter characteds (def: ''). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer","Frame buffer (Akka Framing) (def: 1M)"),
        ArgLong('_', "throttle","Throttle messages in msec (def: 0)"),

        ArgString('t', "filter","Filter (ex: 'AN-225')"),
        
        ArgString('d', "datastore","datastore [mem,file] (def: file)"),
        
        ArgCmd("validator","Validator pipeline"),
        ArgCmd("rewards","Rewards calculations"),
        
        ArgParam("<params>","")
      ).withExit(1)
    ))

    Console.err.println(s"${c}")

    implicit val config = Config(
      keystore = c.getString("keystore").getOrElse("./keystore/validator-1.json"),
      keystorePass = c.getString("keystore.pass").getOrElse("abcd1234"),
      batchSize = c.getInt("batch.size").getOrElse(10),
      batchWindow = c.getLong("batch.window").getOrElse(1000L),
      protocolOptions = MSG_Options.fromArg(c.getString("proto.options").getOrElse(MSG_Options.defaultArg)),
      
      feed = c.getString("feed").getOrElse("mqtt://localhost:1883"),
      output = c.getString("output").getOrElse(""),
      entity = c.getString("entity").getOrElse("validator"),
      format = c.getString("format").getOrElse(""),

      limit = c.getLong("limit").getOrElse(0),
      freq = c.getLong("freq").getOrElse(0),
      delimiter = c.getString("delimiter").getOrElse("\n"),
      buffer = c.getInt("buffer").getOrElse(1024*1024),
      throttle = c.getLong("throttle").getOrElse(0L),
    
      filter = c.getString("filter").getOrElse("").split(",").toSeq,
      cmd = c.getCmd().getOrElse("validator"),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")

    val datastore = new DataStoreMem()

    config.cmd match {
      case "validator" => {
        val pp = new PipelineValidator(config.feed,config.output,datastore)
        val r = pp.run()
        println(s"r=${r}")
        r match {
          case a:Awaitable[_] => {
            val rr = Await.result(a,Duration.Inf)
            Console.err.println(s"result: ${rr}")
          }
          case akka.NotUsed => 
        }

        Console.err.println(s"Events: ${pp.countObj}")
        sys.exit(0)
      }

      case "rewards" => {
        val rewards = new RewardADSB()
        val datastore = new DataStoreMem()
        val r = rewards.calculate(Instant.now.toEpochMilli(),Instant.now.toEpochMilli(),datastore)
        Console.println(s"${r}")
      }
    }
  }
}
