package io.syspulse.aeroware.adsb.ingest

import java.nio.file.{Path,Paths, Files}

import scala.util.{Try,Failure,Success}
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

import io.prometheus.client.Counter

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import akka.NotUsed

class AdsbIngestFile(dataDir:String,filePattern:String,dataFormat:String,fileLimit:Long,fileSize:Long) extends AdsbIngest {
  
    implicit val adsbRW = upickle.default.macroRW[ADSB_Log]

    def getFileName() = Util.toFileWithTime(filePattern)

    val fileRotateTrigger: () => ByteString => Option[Path] = () => {
      var currentFilename: Option[String] = None
      var init = false
      val max = 10 * 1024 * 1024
      var count: Long = 0L
      var size: Long = 0L
      var currentTs = System.currentTimeMillis 
      (element: ByteString) => {
        if(init && (count < fileLimit && size < fileSize)) {
          count = count + 1
          size = size + element.size
          None
        } else {
          currentFilename = Some(getFileName())
          val outputPath = s"${Util.toDirWithSlash(dataDir)}${getFileName()}"
          log.info(s"Writing -> File(${outputPath})...")
          count = 0L
          size = 0L
          init = true
          Some(Paths.get(outputPath))
        }
      }
    }

    val sinkRestartable = if(filePattern!="NONE") { 
      RestartSink.withBackoff(retrySettings) { () =>
        LogRotatorSink(fileRotateTrigger)
      }
    } else Sink.ignore


    val formatFlow = Flow[ADSB_Log].map(a => {
      dataFormat.trim.toLowerCase match {
        case "json" => s"${upickle.default.write(a)}\n"
        case "csv" | "" => s"${Util.toCSV(a.asInstanceOf[ADSB_Log])}\n"    
      }
    })

    def run(config:Config) = {
      val adsbSource = flow(config)
      
      val adsbFlow = adsbSource
        .via(logFlow)
        .via(formatFlow)
        .map(ByteString(_))
        .log(s"output -> File(${getFileName()})")
        .toMat(sinkRestartable)(Keep.both)
        .run()

      adsbFlow
    }
    
}
