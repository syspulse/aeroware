package io.syspulse.aeroware.adsb.ingest

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger
import scala.concurrent.{Await, ExecutionContext, Future}

import java.util.concurrent.TimeUnit
import scala.concurrent.Awaitable

import io.syspulse.skel
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.aeroware.adsb.ingest.flow.PipelineADSB

case class Config(  
  
  feed:String = "",
  output:String = "",
  
  limit:Long = 0L,
  freq: Long = 0L,
  delimiter:String = "",
  buffer:Int = 0,
  throttle:Long = 0L,
  
  entity:String = "",
  filter:Seq[String] = Seq(),
  format:String = "",
    
  datastore:String = "",

  cmd:String = "",
  params: Seq[String] = Seq(),
  sinks:Seq[String] = Seq()
)

object App {
  
  def main(args:Array[String]):Unit = {
    Console.err.println(s"args: '${args.mkString(",")}'")

    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-ingest","",
                
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
        
        ArgCmd("ingest","Ingest pipeline"),
        
        ArgParam("<params>","")
      ).withExit(1)
    ))

    val config = Config(
      
      feed = c.getString("feed").getOrElse(""),
      output = c.getString("output").getOrElse(""),
      entity = c.getString("entity").getOrElse("all"),
      format = c.getString("format").getOrElse(""),

      limit = c.getLong("limit").getOrElse(0),
      freq = c.getLong("freq").getOrElse(0),
      delimiter = c.getString("delimiter").getOrElse("\n"),
      buffer = c.getInt("buffer").getOrElse(1024*1024),
      throttle = c.getLong("throttle").getOrElse(0L),     
    
      filter = c.getString("filter").getOrElse("").split(",").toSeq,
      
      datastore = c.getString("datastore").getOrElse("stdout"),
      
      cmd = c.getCmd().getOrElse("ingest"),
      
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")

    config.cmd match {
      case "ingest" => {
        val pp = config.entity match {
          case "adsb" | "all" =>
            new PipelineADSB(config.feed,config.output)(config)
          
          case _ =>  Console.err.println(s"Uknown entity: '${config.entity}'"); sys.exit(1)
        } 

        val r = pp.run()
        println(s"r=${r}")
        r match {
          case a:Awaitable[_] => {
            val rr = Await.result(a,FiniteDuration(30,TimeUnit.MINUTES))
            Console.err.println(s"result: ${rr}")
          }
          case akka.NotUsed => 
        }

        Console.err.println(s"Events: ${pp.countObj}")
        sys.exit(0)
      }

    }
  }
}