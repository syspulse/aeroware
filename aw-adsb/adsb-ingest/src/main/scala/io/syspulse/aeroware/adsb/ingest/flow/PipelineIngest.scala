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
//import io.syspulse.skel.serde.Parq._
import io.syspulse.skel.serde.ParqCodecTypedSerializable
import com.github.mjakubowski84.parquet4s.{ParquetRecordEncoder,ParquetSchemaResolver}
import io.syspulse.aeroware.adsb.ingest.AdsbIngestedJsonProtocol._

import java.util.concurrent.TimeUnit

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline
import io.syspulse.skel.ingest.flow.Flows

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.ingest.Config
import io.syspulse.aeroware.adsb.ingest.Dump1090URI
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingested
import java.net.InetSocketAddress
import akka.stream.scaladsl.RestartSource
import akka.stream.OverflowStrategy
import akka.stream.RestartSettings

import io.syspulse.skel.Ingestable

object PipelineIngestParq {
  implicit val (parqCodecs,parqTypes) = ParqCodecTypedSerializable.forClass[ADSB]
}

import PipelineIngestParq._
import io.syspulse.skel.serde.Parq._

abstract class PipelineIngest[T](feed:String,output:String)(implicit config:Config)
  extends Pipeline[T,ADSB,ADSB_Ingested](feed,output,config.throttle,config.delimiter,config.buffer,format=config.format) {

  //protected val log = Logger(s"${this}")

  val connectTimeout = config.timeoutConnect
  val idleTimeout = config.timeoutIdle
  
  def filter:Seq[String] = config.filter

  override def formatter[O <: Ingestable](o:O,format:String,nl:String="")(implicit fmt:JsonFormat[O]):ByteString = {
    val adsb = o.asInstanceOf[ADSB_Ingested].adsb
    format match {
      case "adsb" => 
        ByteString(s"${adsb.ts} ${adsb.raw}${nl}")
      case _ => super.formatter(o,format,nl)
    }
  }
    
  override def source() = {
    feed.split("://").toList match {
      case "dump1090" :: _ => {
        val uri = Dump1090URI(feed)
        fromTcpClient(uri.host,uri.port.toInt)
      }
      case _ => super.source()
    }
  }

  //override def process:Flow[T,T,_] = Flow[T].map(v => v)
  
  def transform(a: ADSB): Seq[ADSB_Ingested] = {    
    Seq(ADSB_Ingested(a))
  }
}
