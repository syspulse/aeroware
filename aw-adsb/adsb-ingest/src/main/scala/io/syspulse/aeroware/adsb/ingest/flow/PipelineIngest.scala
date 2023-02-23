package io.syspulse.aeroware.adsb.ingest.flow

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.javadsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

import akka.stream.scaladsl.Tcp

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.ingest.Config
import io.syspulse.aeroware.adsb.ingest.Dump1090URI
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingested
import java.net.InetSocketAddress
import akka.stream.scaladsl.RestartSource
import akka.stream.OverflowStrategy

abstract class PipelineIngest[T](feed:String,output:String)(implicit config:Config,fmt:JsonFormat[ADSB_Ingested])
  extends Pipeline[T,T,ADSB_Ingested](feed,output,config.throttle,config.delimiter,config.buffer)(fmt) {

  protected val log = Logger(s"${this}")

  val connectTimeout = config.timeoutConnect//1000L
  val idleTimeout = config.timeoutIdle //1000L
  
  def filter:Seq[String] = config.filter

  def fromTcp(host:String,port:Int) = {
    val ip = InetSocketAddress.createUnresolved(host, port)
    val conn = Tcp().outgoingConnection(
      remoteAddress = ip,
      connectTimeout = Duration(connectTimeout,TimeUnit.MILLISECONDS),
      idleTimeout = Duration(idleTimeout,TimeUnit.MILLISECONDS)
    )
    val sourceRestarable = RestartSource.withBackoff(retrySettings) { () => 
      log.info(s"Connecting -> dump1090(${host}:${port})...")
      Source.actorRef(1, OverflowStrategy.fail)
        .via(conn)
        .log("dump1090")
    }
    sourceRestarable
  }
    
  override def source() = {
    feed.split("://").toList match {
      case "dump1090" :: _ => {
        val uri = Dump1090URI(feed)
        fromTcp(uri.host,uri.port.toInt)
      }
      case _ => super.source()
    }
  }

  override def process:Flow[T,T,_] = Flow[T].map(v => v)

}
