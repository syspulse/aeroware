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

import io.syspulse.aeroware.adsb.mesh.rewards._
import io.syspulse.aeroware.adsb.mesh.store._
import io.syspulse.aeroware.aircraft.icao.AircraftICAORegistry


case class Config (
  feed:String = "mqtt://localhost:1883",
  output:String = "",

  keystore:String = "./keystore/validator-1.json",
  keystorePass:String = "abcd1234",
  blockSize: Int = 3,
  blockWindow: Long = 1000L,
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,
  
  fanoutWindow: Long = 3000L,

  entity:String = "adsb",
  format:String = "",

  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "\n",
  buffer:Int = 1024*1024,
  throttle:Long = 0L,

  RawStore:String = "mem://",

  validation:Seq[String] = Seq("sig,data,payload,blacklist,blacklist.ip"),
  blacklistAddr:Seq[String] = Seq(),
  blacklistIp:Seq[String] = Seq(),

  id:String = System.currentTimeMillis().toString,

  timeoutConnect:Long = 1000L,
  timeoutIdle:Long = 15000L,
  timeoutRetry:Long = 5000L,

  cmd:String = "validator",
  params: Seq[String] = Seq(),
)

object App extends skel.Server {

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
        
        // ArgInt('_', "block.size",s"ADSB Block max size (def: ${d.blockSize})"),
        // ArgLong('_', "block.window",s"ADSB Block time window (msec) (def: ${d.blockWindow})"),
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
        
        ArgString('d', "RawStore",s"RawStore [mem://,file://, parq://] (def: ${d.RawStore})"),

        ArgString('v', "validation",s"What to validated (def: ${d.validation})"),

        ArgString('_', "blacklist.addr",s"Address blacklist (def: ${d.blacklistAddr})"),
        ArgString('_', "blacklist.ip",s"IP blacklist (def: ${d.blacklistIp})"),

        ArgLong('_', "fanout.window",s"Window to group output data (msec) (def: ${d.fanoutWindow})"),

        ArgString('_', "id",s"Validator ID (unique over restarts) (def: ${d.id})"),

        ArgLong('_', "timeout.idle",s"Idle connection timeout in msec (def: ${d.timeoutIdle})"),
        ArgLong('_', "timeout.connect",s"Connection timeout in msec (def: ${d.timeoutConnect})"),
        ArgLong('_', "timeout.retry",s"Retry timeout in msec (def: ${d.timeoutRetry})"),
        
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

      fanoutWindow = c.getLong("fanout.window").getOrElse(d.fanoutWindow),
      
      feed = c.getString("feed").getOrElse(d.feed),
      output = c.getString("output").getOrElse(d.output),

      entity = c.getString("entity").getOrElse(d.entity),
      format = c.getString("format").getOrElse(d.format),

      limit = c.getLong("limit").getOrElse(d.limit),
      freq = c.getLong("freq").getOrElse(d.freq),
      delimiter = c.getString("delimiter").getOrElse(d.delimiter),
      buffer = c.getInt("buffer").getOrElse(d.buffer),
      throttle = c.getLong("throttle").getOrElse(d.throttle),
          
      RawStore = c.getString("RawStore").getOrElse(d.RawStore),
      validation = c.getListString("validation",d.validation),

      blacklistAddr = c.getListString("blacklist.addr",d.blacklistAddr),
      blacklistIp = c.getListString("blacklist.ip",d.blacklistIp),

      id = c.getString("id").getOrElse(d.id),

      timeoutIdle = c.getLong("timeout.idle").getOrElse(d.timeoutIdle),
      timeoutConnect = c.getLong("timeout.connect").getOrElse(d.timeoutConnect),
      timeoutRetry = c.getLong("timeout.retry").getOrElse(d.timeoutRetry),

      cmd = c.getCmd().getOrElse(d.cmd),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")
    
    val store = config.RawStore.split("://").toList match {
      case "parq" :: dir :: Nil => new RawStoreLake(dir)
      case "parq" :: Nil => new RawStoreLake()
      case "mem" :: Nil | "cache" :: Nil => new RawStoreMem()
      case _ => {
        Console.err.println(s"Uknown RawStore: '${config.RawStore}'")
        sys.exit(1)
      }
    }

    log.info(s"Datstore: ${store}")        

    config.cmd match {
      case "validator" => {
        val pp = new PipelineValidator(config.feed,config.output,store)
        val r = pp.run()
        Console.err.println(s"r=${r}")
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
        val RawStore = new RawStoreMem()
        val r = rewards.calculate(Instant.now.toEpochMilli(),Instant.now.toEpochMilli(),RawStore)
        Console.println(s"${r}")
      }
    }
  }
}
