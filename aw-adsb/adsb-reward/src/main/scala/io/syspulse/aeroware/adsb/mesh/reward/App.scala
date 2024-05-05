package io.syspulse.aeroware.adsb.mesh.reward

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

import io.syspulse.aeroware.adsb.mesh.reward._
import io.syspulse.aeroware.adsb.mesh.reward.engine._
import scala.util.Failure
import scala.util.Success

case class Config (
  
  protocolOptions:Int = MSG_Options.V_1 | MSG_Options.O_EC,
    
  entity:String = "any",
  format:String = "csv",
  
  datastore:String = "file://lake",
  engine:String = "spark://",

  validation:Seq[String] = Seq("ts,sig,data,payload,blacklist,blacklist.ip"),
  blacklistAddr:Seq[String] = Seq(),
  blacklistIp:Seq[String] = Seq(),
  toleranceTs:Long = 750L,

  cmd:String = "reward",
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
      new ConfigurationArgs(args,"adsb-reward","",
        
        ArgString('_', "proto.options",s"Protocol options (def: ${d.protocolOptions})"),
        
        ArgString('e', "entity",s"Ingest entity: (def: ${d.entity})"),        
        ArgString('_', "format",s"Outptu format (none,json,csv) (def: ${d.format})"),
        
        ArgString('d', "datastore",s"datastore [mem://,file://, parq://] (def: ${d.datastore})"),
        ArgString('_', "engine",s"Engine [spark://] (def: ${d.engine})"),

        ArgString('v', "validation",s"What to validated (def: ${d.validation})"),

        ArgString('_', "blacklist.addr",s"Address blacklist (def: ${d.blacklistAddr})"),
        ArgString('_', "blacklist.ip",s"IP blacklist (def: ${d.blacklistIp})"),
        ArgLong('_', "tolerance.ts",s"Timestamp validation tolerance in msec (def: ${d.toleranceTs})"),
        
        ArgCmd("reward","Rewards calculations"),
        
        ArgParam("<params>",""),
        ArgLogging()
      ).withExit(1)
    )).withLogging()

    Console.err.println(s"${c}")

    implicit val config = Config(
      protocolOptions = MSG_Options.fromArg(c.getString("proto.options")).getOrElse(d.protocolOptions),

      entity = c.getString("entity").getOrElse(d.entity),
      format = c.getString("format").getOrElse(d.format),
         
      datastore = c.getString("datastore").getOrElse(d.datastore),
      
      validation = c.getListString("validation",d.validation),
      blacklistAddr = c.getListString("blacklist.addr",d.blacklistAddr),
      blacklistIp = c.getListString("blacklist.ip",d.blacklistIp),
      toleranceTs = c.getLong("tolerance.ts").getOrElse(d.toleranceTs),

      cmd = c.getCmd().getOrElse(d.cmd),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")
    
    val store = config.datastore.split("://").toList match {
      case dir :: Nil => dir
      case "file" :: dir :: Nil => dir
      case "parq" :: dir :: Nil => dir
      case _ => {
        Console.err.println(s"Uknown datastore: '${config.datastore}'")
        sys.exit(1)
      }
    }

    val engine = config.engine.split("://").toList match {
      case "spark" :: Nil => new RewardSpark(store)
      case _ => {
        Console.err.println(s"Uknown engine: '${config.engine}'")
        sys.exit(2)
      }
    }

    log.info(s"Datstore: ${store}")
    log.info(s"Engine: ${engine}")

    val r = config.cmd match {
      case "reward" => {        
        
        val rr = engine.calculateRewards()
        rr match {
          case Success(rr) => rr.miners.mkString("\n")
          // case Success(rr) => rr.miners.map(Util.toCSV(_)).mkString("\n")
          case Failure(e) => e
        }
      }
      case _ =>
        Console.err.println(s"Unknown command: ${config.cmd}")
        sys.exit(3)
    }

    println(s"${r}")
  }
}
