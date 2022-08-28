package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}
import java.net.URI

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger


import io.syspulse.skel
import io.syspulse.skel.config._

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.data.AircraftICAORegistry

case class Config (
  host: String = "0.0.0.0",
  port: Int = 8080,
  uri: String = "/api/v1/adsb",

  dumpHost: String = "localhost",
  dumpPort: Int = 30002,
  fileLimit: Long = 1000000L,
  fileSize: Long = 1024L * 1024L * 10L,
  filePattern: String = "ADSB-{yyyy-MM-dd'T'HH:mm:ssZ}.log",
  connectTimeout: Long = 3000L,
  idleTimeout: Long = 60000L,
  dataDir:String = "/data",
  dataFormat:String = "json",
  trackAircraft:String = "",
  pipes:Seq[String] = Seq()
)

object AppPlayer {
  def main(args: Array[String]):Unit = {
    Console.err.println(s"args: ${args.size}: ${args.toSeq}")

    val default = Config()

    val c = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-player","",
        ArgString('h', "http.host","listen host (def: 0.0.0.0)"),
        ArgInt('p', "http.port","listern port (def: 8080)"),
        ArgString('u', "http.uri","api uri (def: /api/v1/adsb)"),
        
        ArgString('_', "data.format","Data format (json|csv) (def: json)"),
        ArgLong('l', "limit","Limit ADSB events per file"),
        ArgString('a', "aircraft","Aircraft(s) tracker (icaoType,callSign,icaoId). RexExp (e.g. '[Aa][nN].*' - Track All AN"),
                
        ArgParam("pipes","Pipeline of commands")
      ).withExit(1)
    ))

    implicit val config = Config(
      host = c.getString("http.host").getOrElse(default.host),
      port = c.getInt("http.port").getOrElse(default.port),
      uri = c.getString("http.uri").getOrElse(default.uri),
            
      dataFormat = c.getString("data.format").getOrElse(default.dataFormat),
      fileLimit = c.getLong("limit").getOrElse(default.fileLimit),
            
      trackAircraft = c.getString("aircraft").getOrElse(default.trackAircraft),

      //cmd = c.getCmd().getOrElse("ingest"),
      pipes = c.getParams(),
    )

    // val builder = OParser.builder[Config]
    // val parser1 = {
    //   import builder._
    //   OParser.sequence(programName("adsb-player"),head("ADSB Player", ""),
      
    //     opt[String]('j', "data-format").action((x, c) => c.copy(dataFormat = x)).text("Data format (json|csv) (def: json)"),
    //     opt[Long]('l', "limit").action((x, c) => c.copy(fileLimit = x)).text("Limit ADSB events per file"),
        
    //     opt[String]('a', "aircraft").action((x, c) => c.copy(trackAircraft = x)).text("Aircraft(s) tracker (icaoType,callSign,icaoId). RexExp (e.g. '[Aa][nN].*' - Track All AN"),
    //     arg[String]("<file>...").unbounded().optional()
    //       .action((x, c) => c.copy(files = c.files :+ x))
    //       .text("ADSB log files (json/csv)"),
    //       note("" + sys.props("line.separator")),
    //   )
    // }

    Console.err.println(s"${config}")

    val confuration = Configuration.withPriority(Seq(new ConfigurationEnv,new ConfigurationAkka))
    
    var pipe = List[Pipe]()
          
    for(f <- config.pipes) {
      Console.err.println(s"Creating pipe: ${f}")

      val p = {
        if(f.toLowerCase.startsWith("tcp://")) {
          val (uri,interval) = f.split(",") match {
            case Array(uri,interval) => (uri,interval.trim.toLong)
            case Array(uri) => (uri,1L)
            case _ => { Console.err.println(s"Missing URI: ${f}"); ("",0L) }
          }
          val uriParts = new URI(uri.toLowerCase)
          val (tcpHost,tcpPort) = (uriParts.getHost,uriParts.getPort)
          new PipeTcpServer(tcpHost,tcpPort)
        } 
        else
        if(f.toLowerCase.startsWith("ws://")) {
          val (uri,interval) = f.split(",") match {
            case Array(uri,interval) => (uri,interval.trim.toLong)
            case Array(uri) => (uri,1L)
            case _ => { Console.err.println(s"Missing URI: ${f}"); ("",0L) }
          }
          val uriParts = new URI(uri.toLowerCase)
          val (wsHost,wsPort) = (uriParts.getHost,uriParts.getPort)
          new PipeRadarWS(wsHost,wsPort,interval)
        } 
        else
        if(f.toLowerCase == "stdout" || f.toLowerCase == "print") {
          new PipePrint
        }
        else
        if(f.toLowerCase.startsWith("position")) {
          val interval = f.split("[()]") match {
            case Array(_,interval) => interval.toLong
            case Array(_) => 1L
            case _ => 1L
          }
          new PipePosition(interval)
        }
        else
        if(f.toLowerCase == "delay") {
          new PipeDelay
        }
        else
        if(f.toLowerCase.startsWith("repeat")) {
          val count = f.split("[()]") match {
            case Array(_,count) => count.toInt
            case Array(_) => Int.MaxValue
            case _ => 0
          }
          new PipeRepeat(count)
        }
        else
        if(f.toLowerCase.startsWith("sleep")) {
          val interval = f.split("[()]") match {
            case Array(_,interval) => interval.toLong
            case Array(_) => 1000L
            case _ => 0L
          }
          new PipeSleep(interval)
        }
        else
        if(f.toLowerCase.startsWith("radar")) {
          val interval = f.split("[()]") match {
            case Array(_,interval) => interval.toLong
            case Array(_) => 1000L
            case _ => 1000L
          }
          new PipeRadar(interval)
        }
        else 
        f.toLowerCase().split("\\.").last match {
          case "csv" => new PipeInputCSV(f)
          case "adsb" => new PipeInputADSB(f)
          case _ => {
            Console.err.println(s"format unknown: ${f}, trying as ADSB")
            //new PipeNone
            new PipeInputADSB(f)
          }
        }
      }

      pipe = pipe :+ p
    }
    
    Console.err.println(s"Aircrafts Registry: loading...")
    AircraftICAORegistry.sync()
    Console.err.println(s"Aircrafts Registry: ${AircraftICAORegistry.size}")

    Console.err.println(s"Pipe: ${pipe}")

    var finished = false
    var a:Try[ADSB] = Success(ADSB_Continue())
    
    do {
      for( p <- pipe ) {
        a = p.flow(a) 
      }

      if(a.isFailure) {
        //Console.err.println(s"FAILURE ====> ${a}: ${a.toEither.left.get.getClass}")
        if(! a.toEither.left.get.isInstanceOf[java.util.NoSuchElementException]) {
          Console.err.println(s"{a}: ${a.get}")
        }
        finished = true; 
      }

      
    } while (!finished)

    System.exit(0)
  }
}
