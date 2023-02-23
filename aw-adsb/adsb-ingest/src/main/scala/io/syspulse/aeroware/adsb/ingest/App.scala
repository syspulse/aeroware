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

import io.syspulse.aeroware.adsb.ingest.flow.{ PipelineADSB, PipelineDump1090}

case class Config(  
  host:String="0.0.0.0",
  port:Int=8080,
  uri:String = "/api/v1/adsb",

  feed:String = "dump1090://rp-1:30002",
  output:String = "stdout://",
  
  limit:Long = -1,
  delimiter:String = "",
  buffer:Int = 1024*1024,
  throttle:Long = 0L,
  
  entity:String = "dump1090",
  filter:Seq[String] = Seq(),
  format:String = "dump1090",

  timeoutConnect:Long = 3000L,
  timeoutIdle:Long = 1000 * 60 * 60 * 24L,
    
  datastore:String = "",

  cmd:String = "ingest",
  params: Seq[String] = Seq(),  
)

object App {
  
  def main(args:Array[String]):Unit = {
    Console.err.println(s"args: ${args.size}: ${args.toSeq}")

    val d = Config()
    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-ingest","",
        ArgString('h', "http.host",s"listen host (def: ${d.host})"),
        ArgInt('p', "http.port",s"listern port (def: ${d.port})"),
        ArgString('u', "http.uri",s"api uri (def: ${d.uri})"),

        ArgString('f', "feed",s"Input Feed (dump1090://host:port, file://, kafka://, (def: stdin://) (def=${d.feed})"),
        ArgString('o', "output",s"Output file (pattern is supported: data-{yyyy-MM-dd-HH-mm}.log) (def=${d.output})"),
            
        ArgString('_', "format",s"Output format (json,csv,adsb) (def: ${d.format})"),

        ArgString('_', "delimiter",s"""Delimiter characteds (def: '${d.delimiter}'). Usage example: --delimiter=`echo -e $"\r\n"`"""),
        ArgInt('_', "buffer",s"Frame buffer (Akka Framing) (def: ${d.buffer})"),
        ArgLong('n', s"limit",s"File Limit (def: ${d.limit})"),

        // ArgLong('s', s"size",s"File Size Limit (def: ${d.size})"),
        // ArgLong('_', "freq","Frequency"),
        // ArgString('_', "delimiter","""Delimiter characteds (def: ''). Usage example: --delimiter=`echo -e $"\r"` """),
        // ArgInt('_', "buffer","Frame buffer (Akka Framing) (def: 1M)"),
        // ArgLong('_', "throttle","Throttle messages in msec (def: 0)"),

        ArgString('e', "entity",s"Ingest entity: (adsb,dump1090) (def: ${d.entity})"),               
        ArgString('a', "aircraft",s"Filter (ex: 'AN-225') (def=${d.filter})"),
        ArgString('_', "timeout.connect",s"Connection timeout (def: ${d.timeoutConnect})"),
        ArgString('_', "timeout.idle",s"Idle timeout (def: ${d.timeoutIdle})"),
        
        ArgString('d', "datastore",s"datastore [elastic,stdout,file] (def: ${d.datastore})"),
        
        ArgCmd("ingest","Ingest pipeline"),
        
        ArgParam("<params>","")
      ).withExit(1)
    ))

    val config = Config(
      
      host = c.getString("http.host").getOrElse(d.host),
      port = c.getInt("http.port").getOrElse(d.port),
      uri = c.getString("http.uri").getOrElse(d.uri),
      
      feed = c.getString("feed").getOrElse(d.feed),
      output = c.getString("output").getOrElse(d.output),
      datastore = c.getString("datastore").getOrElse(d.datastore),

      limit = c.getLong("limit").getOrElse(d.limit),
      // size = c.getLong("size").getOrElse(d.size),
      
      delimiter = c.getString("delimiter").getOrElse(d.delimiter),
      buffer = c.getInt("buffer").getOrElse(d.buffer),
      throttle = c.getLong("throttle").getOrElse(d.throttle),
      
      entity = c.getString("entity").getOrElse(d.entity),
      format = c.getString("output.format").getOrElse(d.format),
      filter = c.getListString("aircraft"),
      timeoutConnect = c.getLong("timeout.connect").getOrElse(d.timeoutConnect),
      timeoutIdle = c.getLong("timeout.idle").getOrElse(d.timeoutIdle),
            
      cmd = c.getCmd().getOrElse("ingest"),      
      params = c.getParams(),
    )

    Console.err.println(s"Config: ${config}")

    config.cmd match {
      case "ingest" => {
        val pp = config.entity match {
          case "dump1090" =>
            new PipelineDump1090(config.feed,config.output)(config)

          case "adsb" =>
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