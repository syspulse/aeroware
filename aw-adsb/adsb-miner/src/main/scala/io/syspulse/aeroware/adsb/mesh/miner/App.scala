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
import io.syspulse.aeroware.aircraft.icao.AircraftICAORegistry

case class Config (
  feed:String = "",
  output:String = "mqtt://localhost:1883",

  keystore:String = "./keystore/miner-1.json",
  keystorePass:String = "test123",
  
  blockSize: Int = 2,
  blockWindow: Long = 1000L,
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,

  entity:String = "adsb",
  format:String = "",

  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "\n",
  buffer:Int = 1024*1024,
  throttle:Long = 0L,

  timeoutConnect:Long = 3000L,
  timeoutIdle:Long = 60000L,
  timeoutRetry:Long = 10000L,

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
      new ConfigurationArgs(args,"aw-miner","",
        
        ArgString('_', "keystore.file",s"Keystore file (def: ${d.keystore})"),
        ArgString('_', "keystore.pass",s"Keystore password"),        
        
        ArgInt('_', "block.size",s"ADSB Block max size (def: ${d.blockSize})"),
        ArgLong('_', "block.window",s"ADSB Block time window (msec) (def: ${d.blockWindow})"),
        ArgString('_', "proto.options",s"Protocol options (def: ${d.protocolOptions})"),
        
        ArgString('f', "feed",s"Input Feed (def: ${d.feed})"),
        ArgString('o', "output",s"Output file (def: ${d.output})"),

        ArgString('e', "entity",s"mining entity (adsb,notam,metar,..) (def: ${d.entity})"),        
        ArgString('_', "format",s"format () (def: ${d.format})"),

        ArgLong('_', "limit",s"Limit (def: ${d.limit})"),
        ArgLong('_', "freq",s"Frequency (def: ${d.feed})"),
        ArgString('_', "delimiter",s"""Delimiter characteds (def: '${d.delimiter}'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),

        ArgLong('_', "timeout.idle",s"Idle connection timeout in msec (def: ${d.timeoutIdle})"),
        ArgLong('_', "timeout.connect",s"Connection timeout in msec (def: ${d.timeoutConnect})"),
        ArgLong('_', "timeout.retry",s"Retry timeout in msec (def: ${d.timeoutRetry})"),
                
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

      timeoutIdle = c.getLong("timeout.idle").getOrElse(d.timeoutIdle),
      timeoutConnect = c.getLong("timeout.connect").getOrElse(d.timeoutConnect),
      timeoutRetry = c.getLong("timeout.retry").getOrElse(d.timeoutRetry),
          
      cmd = c.getCmd().getOrElse(d.cmd),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")    
    
    config.cmd match {
      case "miner" => {
                
        val pp = new PipelineMinerADSB(config.feed,config.output)

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
