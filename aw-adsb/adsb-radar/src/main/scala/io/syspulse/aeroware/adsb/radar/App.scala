package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import java.net.URI

import com.typesafe.scalalogging.Logger
import akka.actor.typed.ActorSystem

import scopt.OParser

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb.radar.store._

case class Config (
  feed:String = "mqtt://localhost:1883",
  output:String = "",
  
  format:String = "",
  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "\n",
  buffer:Int = 1024*1024,
  throttle:Long = 0L,

  entity:String = "",

  datastore:String = "mem://",

  cmd:String = "simulator",

  params: Seq[String] = Seq(),
)


object App {
  def main(args: Array[String]):Unit = {

        Console.err.println(s"args: '${args.mkString(",")}'")

    val d = Config()
    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-radar","",
                
        ArgString('f', "feed",s"Input Feed (def: ${d.feed})"),
        ArgString('o', "output",s"Output file (def: ${d.output})"),
        ArgString('e', "entity",s"Ingest entity: (def: ${d.entity})"),
                
        ArgLong('_', "limit",s"Limit (def: ${d.limit})"),
        ArgLong('_', "freq",s"Frequency (def: ${d.feed})"),
        ArgString('_', "delimiter",s"""Delimiter characteds (def: '${d.delimiter}'). Usage example: --delimiter=`echo -e $"\r"` """),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('_', "throttle",s"Throttle messages in msec (def: ${d.throttle})"),
        
        ArgString('d', "datastore",s"datastore [mem://,file://, parq://] (def: ${d.datastore})"),
        
        
        ArgCmd("simulator","Simulator"),
        ArgCmd("server","Sever "),
        
        ArgParam("<params>",""),
        ArgLogging()
      ).withExit(1)
    )).withLogging()

    Console.err.println(s"${c}")

    implicit val config = Config(      
      feed = c.getString("feed").getOrElse(d.feed),
      output = c.getString("output").getOrElse(d.output),                  
      limit = c.getLong("limit").getOrElse(d.limit),
      freq = c.getLong("freq").getOrElse(d.freq),
      delimiter = c.getString("delimiter").getOrElse(d.delimiter),
      buffer = c.getInt("buffer").getOrElse(d.buffer),
      throttle = c.getLong("throttle").getOrElse(d.throttle),
          
      entity = c.getString("entity").getOrElse(d.entity),
      datastore = c.getString("datastore").getOrElse(d.datastore),
      
      cmd = c.getCmd().getOrElse(d.cmd),
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")

    val store = config.datastore.split("://").toList match {
      case "mem" :: Nil | "cache" :: Nil => new RadarStoreMem()
      case _ => {
        Console.err.println(s"Unknown datastore: '${config.datastore}'")
        sys.exit(1)
      }
    }
    
    config.cmd match {
      case "simulator" => 
        val supervisor = actor.AirspaceSupervisor()
        val root = ActorSystem[String](supervisor, "Airspace-System")

        // inject Aircrafts
        root ! "start"
        root ! "random"
        
      case _ => 
        Console.err.println(s"Unknown command: ${config.cmd}")
        sys.exit(0)
    }
        
  }
  
}
