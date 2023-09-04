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
  feed:String = "mqtt://localhost:1883",
  output:String = "",

  keystore:String = "./keystore/validator-1.json",
  keystorePass:String = "abcd1234",
  blockSize: Int = 3,
  blockWindow: Long = 1000L,
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,

  entity:String = "",
  format:String = "",
  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "\n",
  buffer:Int = 1024*1024,
  throttle:Long = 0L,

  datastore:String = "mem://",

  validation:Seq[String] = Seq("sig"),

  cmd:String = "",
  params: Seq[String] = Seq(),
)

object App extends skel.Server {
  import MSG_MinerData._

  def main(args: Array[String]):Unit = {
    Console.err.println(s"args: '${args.mkString(",")}'")

    val d = Config()
    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-validator","",
        
        ArgString('_', "keystore.file",s"Keystore file (def: ${d.keystore})"),
        ArgString('_', "keystore.pass",s"Keystore password"),        
        
        ArgInt('_', "block.size",s"ADSB Block max size (def: ${d.blockSize})"),
        ArgLong('_', "block.window",s"ADSB Block time window (msec) (def: ${d.blockWindow})"),
        ArgString('_', "proto.options",s"Protocol options (def: ${d.protocolOptions})"),
        
        ArgString('f', "feed",s"Input Feed (def: ${d.feed})"),
        ArgString('o', "output",s"Output file (def: ${d.output})"),
        ArgString('e', "entity",s"Ingest entity: (def: ${d.entity})"),
        
        ArgString('_', "format",s"Outptu format (none,json,csv) (def: ${d.format})"),

        ArgLong('_', "limit",s"Limit (def: ${d.limit})"),
        ArgLong('_', "freq",s"Frequency (def: ${d.feed})"),
        ArgString('_', "delimiter",s"""Delimiter characteds (def: '${d.delimiter}'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),
        
        ArgString('d', "datastore",s"datastore [mem://,file://, parq://] (def: ${d.datastore})"),

        ArgString('v', "validation",s"What to validated (def: ${d.validation})"),
        
        ArgCmd("validator","Validator pipeline"),
        ArgCmd("rewards","Rewards calculations"),
        
        ArgParam("<params>",""),
        ArgLogging()
      ).withExit(1)
    )).withLogging()

    Console.err.println(s"${c}")

    implicit val config = Config(
      keystore = c.getString("keystore.file").getOrElse(d.keystore),
      keystorePass = c.getString("keystore.pass").getOrElse(d.keystorePass),
      blockSize = c.getInt("block.size").getOrElse(d.blockSize),
      blockWindow = c.getLong("block.window").getOrElse(d.blockWindow),
      protocolOptions = MSG_Options.fromArg(c.getString("proto.options")).getOrElse(d.protocolOptions),
      
      feed = c.getString("feed").getOrElse(d.feed),
      output = c.getString("output").getOrElse(d.output),
      entity = c.getString("entity").getOrElse(d.entity),
      format = c.getString("format").getOrElse(d.format),

      limit = c.getLong("limit").getOrElse(d.limit),
      freq = c.getLong("freq").getOrElse(d.freq),
      delimiter = c.getString("delimiter").getOrElse(d.delimiter),
      buffer = c.getInt("buffer").getOrElse(d.buffer),
      throttle = c.getLong("throttle").getOrElse(d.throttle),
          
      datastore = c.getString("datastore").getOrElse(d.datastore),
      validation = c.getListString("validation",d.validation),

      cmd = c.getCmd().getOrElse("validator"),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")


    val store = config.datastore.split("://").toList match {
      case "parq" :: dir :: Nil => new DataStoreLake(dir)
      case "parq" :: Nil => new DataStoreLake()
      case "mem" :: Nil | "cache" :: Nil => new DataStoreMem()
      case _ => {
        Console.err.println(s"Uknown datastore: '${config.datastore}'")
        sys.exit(1)
      }
    }

    config.cmd match {
      case "validator" => {
        val pp = new PipelineValidator(config.feed,config.output,store)
        val r = pp.run()
        println(s"r=${r}")
        r match {
          case a:Awaitable[_] => {
            val rr = Await.result(a,Duration.Inf)
            Console.err.println(s"result: ${rr}")
          }
          case akka.NotUsed => 
        }

        Console.err.println(s"Events: ${pp.countObj.get()}")
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
