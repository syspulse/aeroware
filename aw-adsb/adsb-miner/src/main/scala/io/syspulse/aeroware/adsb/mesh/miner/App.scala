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
  output:String = "",

  keystore:String = "",
  keystorePass:String = "",
  
  blockSize: Int = 2,
  blockWindow: Long = 1000L,
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,

  entity:String = "",
  format:String = "",
  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "",
  buffer:Int = 0,
  throttle:Long = 0L,

  filter:Seq[String] = Seq.empty,

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
      new ConfigurationArgs(args,"adsb-miner","",
        
        ArgString('_', "keystore.file","Keystore file (def: ./keystore/)"),
        ArgString('_', "keystore.pass","Keystore password"),        
        ArgInt('_', "block.size","ADSB Block max size"),
        ArgLong('_', "block.window","ADSB Block time window (msec)"),
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
        
        ArgString('d', "datastore","datastore [elastic,stdout,file] (def: stdout)"),
        
        ArgCmd("miner","Miner pipeline"),
        
        ArgParam("<params>",""),
        ArgLogging()
      ).withExit(1)
    )).withLogging()

    Console.err.println(s"${c}")

    implicit val config = Config(
      keystore = c.getString("keystore").getOrElse("./keystore/miner-1.json"),
      keystorePass = c.getString("keystore.pass").getOrElse("test123"),
      blockSize = c.getInt("block.size").getOrElse(2),
      blockWindow = c.getLong("block.window").getOrElse(1000L ),
      protocolOptions = MSG_Options.fromArg(c.getString("proto.options").getOrElse(MSG_Options.defaultArg)),
      
      feed = c.getString("feed").getOrElse(""),
      output = c.getString("output").getOrElse("mqtt://localhost:1883"),
      entity = c.getString("entity").getOrElse("all"),
      format = c.getString("format").getOrElse(""),

      limit = c.getLong("limit").getOrElse(0),
      freq = c.getLong("freq").getOrElse(0),
      delimiter = c.getString("delimiter").getOrElse("\n"),
      buffer = c.getInt("buffer").getOrElse(1024*1024),
      throttle = c.getLong("throttle").getOrElse(0L),
    
      filter = c.getString("filter").getOrElse("").split(",").toSeq,
      cmd = c.getCmd().getOrElse("miner"),
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
