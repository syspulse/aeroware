package io.syspulse.aeroware.adsb.ingest

import java.nio.file.{Path,Paths, Files}

import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl.LogRotatorSink

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
import akka.NotUsed

case class Config (
  host: String = "localhost",
  port: Int = 30002,
  fileLimit: Long = 1000000L,
  fileSize: Long = 1024L * 1024L * 100L,
  filePattern: String = "yyyy-MM-dd'T'HH:mm:ssZ",
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
        opt[Long]('l', "limit").action((x, c) => c.copy(fileLimit = x)).text("Limit ADSB events per file"),
        opt[Long]('s', "size").action((x, c) => c.copy(fileSize = x)).text("Limit ADSB file size"),
        opt[String]('f', "file").action((x, c) => c.copy(filePattern = x)).text("Output file pattern (def=yyyy-MM-dd'T'HH:mm:ssZ)",
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

        val f = DateTimeFormatter.ofPattern(config.filePattern)
        val lastTimestamp = System.currentTimeMillis()

        def getFileName() = {
          val suffix = ZonedDateTime.ofInstant(Instant.now, ZoneId.systemDefault).format(f)
          val outputFile = s"adsb-${suffix}.json"
          outputFile
        }
        val ver = 1

        val triggerCreator: () => ByteString => Option[Path] = () => {
          var currentFilename: Option[String] = None
          var init = false
          val max = 10 * 1024 * 1024
          var count: Long = 0L
          var size: Long = 0L
          var currentTs = System.currentTimeMillis 
          (element: ByteString) => {
            if(init && (count < config.fileLimit && size < config.fileSize)) {
              count = count + 1
              size = size + element.size
              None
            } else {
              currentFilename = Some(getFileName())
              val outputPath = s"${config.dataDir}/${getFileName()}"
              log.info(s"Writing -> File(${outputPath})...")
              count = 0L
              size = 0L
              init = true
              Some(Paths.get(outputPath))
            }
          }
        }


        val sinkRestartable = RestartSink.withBackoff(retrySettings) { () =>
          //FileIO.toPath(Paths.get(outputPath))
          LogRotatorSink(triggerCreator)
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
