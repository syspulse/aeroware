package io.syspulse.aeroware.adsb.ingest.old

import com.typesafe.scalalogging.Logger

import scopt.OParser

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import akka.NotUsed

object AppOld extends skel.Server {
  def main(args: Array[String]):Unit = {

    println(s"args: ${args.size}: ${args.toSeq}")

    val builder = OParser.builder[Config]
    val parser1 = {
      import builder._
      OParser.sequence(programName(Util.info._1), head(Util.info._1, Util.info._2),
        opt[String]('h', "host").action((x, c) => c.copy(httpHost = x)).text("Listen address"),  
        opt[Int]('p', "port").action((x, c) => c.copy(httpPort = x)).text("Listen port"),
        opt[String]('u', "uri").action((x, c) => c.copy(httpUri = x)).text("Uri (/api/v1/adsb)"),

        opt[String]('o', "dump1090.host").action((x, c) => c.copy(dumpHost = x)).text("dump1090 Host"),  
        opt[Int]('r', "dump1090.port").action((x, c) => c.copy(dumpPort = x)).text("dump1090 port"),
        
        opt[String]('d', "data.dir").action((x, c) => c.copy(dataDir = x)).text("Data directory (def: /data)"),
        opt[String]('j', "data.format").action((x, c) => c.copy(dataFormat = x)).text("Data format (json|csv) (def: json)"),

        opt[Long]('l', "limit").action((x, c) => c.copy(fileLimit = x)).text("Limit ADSB events per file"),
        opt[Long]('s', "size").action((x, c) => c.copy(fileSize = x)).text("Limit ADSB file size"),
        opt[String]('f', "file").action((x, c) => c.copy(filePattern = x)).text("Output file pattern (def=ADSB-{yyyy-MM-dd'T'HH:mm:ssZ}.log  use 'NONE' for no Sinking)"),
        opt[Long]('c', "connect").action((x, c) => c.copy(connectTimeout = x)).text("connect timeout"),
        opt[Long]('i', "idle").action((x, c) => c.copy(idleTimeout = x)).text("idle timeout"),
        opt[String]('a', "aircraft").action((x, c) => c.copy(trackAircraft = x)).text("Aircraft(s) tracker (icaoType,callSign,icaoId). RexExp (e.g. '[Aa][nN].*' - Track All AN"),
        arg[String]("<args>...").unbounded().optional()
          .action((x, c) => c.copy(args = c.args :+ x))
          .text("optional args"),
          note("" + sys.props("line.separator")),
      )
    }

    OParser.parse(parser1, args, Config()) match {
      case Some(config) => {
        val configuration = Configuration.default

        println(s"${config}")

        (new AdsbIngestFile(config.dataDir,config.filePattern,config.dataFormat,config.fileLimit,config.fileSize)).run(config)

        run( config.httpHost, config.httpPort, config.httpUri, configuration, Seq())
      }
      case _ =>
        System.exit(1)
    }
  }
}
