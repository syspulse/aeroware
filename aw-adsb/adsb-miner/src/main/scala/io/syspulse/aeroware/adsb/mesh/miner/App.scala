package io.syspulse.aeroware.adsb.mesh.miner

import com.typesafe.scalalogging.Logger

import scala.concurrent.Awaitable
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb.core.ADSB

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Options
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

case class Config (
  feed:String = "",
  output:String = "mqtt://localhost:1883",

  keystore:String = "./keystore/miner-1.json",
  keystorePass:String = "test123",
  
  blockSize: Int = 2,
  blockWindow: Long = 1000L,
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,

  entity:String = "",
  format:String = "",

  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "\n",
  buffer:Int = 1024*1024,
  throttle:Long = 0L,

  cmd:String = "miner",
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
        ArgString('_', "format",s"Format () (def: ${d.format})"),

        ArgLong('_', "limit",s"Limit (def: ${d.limit})"),
        ArgLong('_', "freq",s"Frequency (def: ${d.feed})"),
        ArgString('_', "delimiter",s"""Delimiter characteds (def: '${d.delimiter}'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),
                
        ArgCmd("miner","Miner pipeline"),
        
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
          
      cmd = c.getCmd().getOrElse(d.cmd),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")

    config.cmd match {
      case "miner" => {
        val pp = new PipelineMiner(config.feed,config.output)
        val r = pp.run()
        Console.err.println(s"r=${r}")
        r match {
          case a:Awaitable[_] => {
            val rr = Await.result(a,FiniteDuration(30,TimeUnit.MINUTES))
            Console.err.println(s"result: ${rr}")
          }
          case akka.NotUsed => 
            Thread.sleep(Long.MaxValue)
          
          case _ => 
            Thread.sleep(Long.MaxValue)
        }

        Console.err.println(s"Events: ${pp.countObj}")
        sys.exit(0)
      }

    }
  }
}
