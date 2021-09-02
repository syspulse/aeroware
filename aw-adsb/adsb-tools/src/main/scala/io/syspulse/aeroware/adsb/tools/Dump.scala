package io.syspulse.aeroware.adsb.tools

import com.typesafe.scalalogging.Logger

import scopt.OParser

import io.syspulse.skel
import io.syspulse.skel.config._

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
  trackAircraft:String = "", // [Aa][nN].* - Antonov track
  files:Seq[String] = Seq()
)

object Dump {
  def main(args: Array[String]):Unit = {

    println(s"args: ${args.size}: ${args.toSeq}")

    val builder = OParser.builder[Config]
    val parser1 = {
      import builder._
      OParser.sequence(programName("adsb-dump"),head("ADSB dump", ""),
      
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

        println(s"${config}")

        val decoder = new Decoder()
              
        for(f <- config.files) {
          Console.err.println(s"File: ${f}")

          val raw = 
            f.toLowerCase().split("\\.").last match {
              case "csv" => scala.io.Source.fromFile(f).getLines().filter(_.trim!="").map( s => s.split(","))
              case "adsb" => scala.io.Source.fromFile(f).getLines().filter(_.trim!="").map( s => s.split("\\s+"))
              case _ => {
                Console.err.println(s"format unknown: ${f}")
                Iterator[Array[String]]()
              }
          }

          //if(raw.size != 0) {
          {
            val data = raw.map( ss => (ss(0),ss(1)))
          
            var a0 = decoder.decode(data.next()._2).get.asInstanceOf[ADSB_AirbornePositionBaro]
            var aLast = a0
            for( d <- data ) {
              val a1 = decoder.decode(d._2).get.asInstanceOf[ADSB_AirbornePositionBaro]
              val loc = Decoder.getGloballPosition(a0,a1)

              println(s"${a1.ts} : ${aLast.ts}: ${a1.ts - aLast.ts}")
              if((a1.ts - aLast.ts) > 2000L) {
                println(s"${loc.lat},${loc.lon},${loc.alt.alt},${a1.ts}")
                aLast = a1
              }
              a0 = a1
            }
          }

      }
        
      }
      case _ =>
        System.exit(1)
    }
  }
}
