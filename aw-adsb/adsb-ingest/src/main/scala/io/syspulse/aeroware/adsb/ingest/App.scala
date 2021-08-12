package io.syspulse.aeroware.adsb.ingest

import java.nio.file.{Paths, Files}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format._

import java.net.InetSocketAddress

import scopt.OParser

import upickle._

import io.syspulse.aeroware.adsb._

case class Config (
  host: String = "localhost",
  port: Int = 30002,
  limit: Long = 1000000L,
  connectTimeout: Long = 3000L,
  idleTimeout: Long = 60000L,
  dataDir:String = "/data",
  args:Seq[String] = Seq()
)

object App {
  def main(args: Array[String]):Unit = {
    val log = Logger(s"${this}")

    println(s"args: ${args.size}: ${args.toSeq}")

    val builder = OParser.builder[Config]
    val parser1 = {
      import builder._
      OParser.sequence(programName("adsb-ingest"),head("ADSB", ""),
        opt[Int]('p', "port").action((x, c) => c.copy(port = x)).text("dump1090 port"),
        opt[String]('h', "host").action((x, c) => c.copy(host = x)).text("dump1090 Host"),
        opt[String]('d', "data").action((x, c) => c.copy(dataDir = x)).text("Data directory"),
        opt[Long]('l', "limit").action((x, c) => c.copy(limit = x)).text("Limit ADSB events per file"),
        opt[Long]('c', "connect").action((x, c) => c.copy(connectTimeout = x)).text("connect timeout"),
        opt[Long]('i', "idle").action((x, c) => c.copy(idleTimeout = x)).text("idle timeout"),
        arg[String]("<args>...").unbounded().optional()
          .action((x, c) => c.copy(args = c.args :+ x))
          .text("optional unbounded args"),
          note("some notes." + sys.props("line.separator")),
      )
    }

    OParser.parse(parser1, args, Config()) match {
      case Some(config) => {
        println(s"${config}")
    
        implicit val system = ActorSystem("ADSB-Ingest")
        implicit val adsbRW = upickle.default.macroRW[ADSB]

         val retrySettings = RestartSettings(
          minBackoff = 1.seconds,
          maxBackoff = 10.seconds,
          randomFactor = 0.1
        )//.withMaxRestarts(6, 1.minutes)

        val conn = InetSocketAddress.createUnresolved(config.host, config.port)
        val connection = Tcp().outgoingConnection(
          remoteAddress = conn,
          connectTimeout = Duration(config.connectTimeout,MILLISECONDS),
          idleTimeout = Duration(config.idleTimeout,MILLISECONDS)
        )

        val source =  Source.actorRef(1, OverflowStrategy.fail)
        val sourceRestarable = RestartSource.withBackoff(retrySettings) { () => 
          log.info(s"Connecting -> dump1090(${config.host}:${config.port})...")
          source
            .via(connection)
            .log("dump1090")
        }

        val f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
        
        val ver = 1

        def getFileName() = {
          val suffix = ZonedDateTime.ofInstant(Instant.now, ZoneId.systemDefault).format(f)
          val outputFile = s"adsb-${suffix}.json"
          outputFile
        }

        val sinkRestartable = RestartSink.withBackoff(retrySettings) { () =>
          val outputPath = s"${config.dataDir}/${getFileName()}"

          log.info(s"Writing -> File(${outputPath})...")

          FileIO.toPath(Paths.get(outputPath))
        }

        val framer = Flow[ByteString].via(Framing.delimiter(ByteString("\n"), 10000, allowTruncation = true))
        val transformer = Flow[ByteString].map(v => { val ts = System.currentTimeMillis; ADSB(ts, ver, v.utf8String) })
        val printer = Flow[ADSB].map(v => { log.debug(s"${v}"); v }).log(s"output -> File(${getFileName()})")
        val jsoner = Flow[ADSB].map(a => s"${upickle.default.write(a)}\n")

        val flow = RestartFlow.withBackoff(retrySettings) { () =>
          framer.via(transformer).via(printer).via(jsoner).map(ByteString(_))
        }

        sourceRestarable.via(flow).toMat(sinkRestartable)(Keep.both).run()

        //Await.result(futureFlow._3, Duration.Inf)
      }
      case _ =>
        System.exit(1)
    }
  }
}
