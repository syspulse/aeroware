package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}
import java.net.URI

import com.typesafe.scalalogging.Logger
import akka.actor.typed.ActorSystem

import scopt.OParser

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb.core._

case class Config (
  httpHost: String = "0.0.0.0",
  httpPort: Int = 8080,
  httpUri: String = "/api/v1/adsb",

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
  files:Seq[String] = Seq()
)

object App {
  def main(args: Array[String]):Unit = {

    println(s"args: ${args.size}: ${args.toSeq}")

    val builder = OParser.builder[Config]
    val parser1 = {
      import builder._
      OParser.sequence(programName(Util.info._1), head(Util.info._1, Util.info._2),
      
        opt[String]('j', "data-format").action((x, c) => c.copy(dataFormat = x)).text("Data format (json|csv) (def: json)"),
        opt[Long]('l', "limit").action((x, c) => c.copy(fileLimit = x)).text("Limit ADSB events per file"),
        
        opt[String]('a', "aircraft").action((x, c) => c.copy(trackAircraft = x)).text("Aircraft(s) tracker (icaoType,callSign,icaoId). RexExp (e.g. '[Aa][nN].*' - Track All AN"),
        arg[String]("<file>...").unbounded().optional()
          .action((x, c) => c.copy(files = c.files :+ x))
          .text("ADSB log files (json/csv)"),
          note("" + sys.props("line.separator")),
      )
    }

    OParser.parse(parser1, args, Config()) match {
      case Some(config) => {
        val confuration = Configuration.withPriority(Seq(new ConfigurationEnv,new ConfigurationAkka))

        Console.err.println(s"${config}")
      
        val supervisor = AirspaceSupervisor()
        val supervisorActor = ActorSystem[String](supervisor, "Airspace-System")

        // inject Aircrafts
        supervisorActor ! "start"
        supervisorActor ! "random"
      }
      case _ =>
        System.exit(1)
    }
  }
}
