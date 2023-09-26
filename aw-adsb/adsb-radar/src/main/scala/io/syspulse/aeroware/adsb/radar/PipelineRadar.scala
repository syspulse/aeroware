package io.syspulse.aeroware.adsb.radar

import scala.util.Random
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
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
import java.net.InetSocketAddress
import akka.stream.scaladsl.RestartSource
import akka.stream.OverflowStrategy

import scala.concurrent.Future
import scala.util.Random
import scala.util.Failure
import scala.util.Success
import akka.stream.scaladsl.BroadcastHub

import akka.Done
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.RestartSink

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw

import io.syspulse.aeroware.adsb.ingest.Dump1090URI
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingested

import io.syspulse.aeroware.adsb.mesh.protocol._

import io.syspulse.skel.util.Util

import akka.NotUsed
import java.util.concurrent.atomic.AtomicLong
import io.syspulse.aeroware.adsb.radar.store.RadarStore


class PipelineRadar(feed:String,output:String,datastore:RadarStore)(implicit config:Config)
  extends Pipeline[MeshData,MeshData,MeshData](feed,output,config.throttle,config.delimiter,config.buffer) {
  
  implicit protected val log = Logger(s"${this}")
  //implicit val ec = system.dispatchers.lookup("default-executor") //ExecutionContext.global
  //implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global       

  override def process = Flow[MeshData].map( m => {
    // push data to datastore
    datastore.<--(m)
    m 
  })

  def parse(data:String):Seq[MeshData] = {    
    log.debug(s"data: ${data}")

    val msgs = if(data.stripLeading().startsWith("{")) {          
      try {        
        val msg = { 
          val msg = data.parseJson.convertTo[MeshData]
          config.past match {
            case -1 => 
              msg.copy(ts = System.currentTimeMillis / 1000L * 1000L + msg.ts % 1000L )
            case 0 => 
              msg
            case p => 
              msg.copy(ts = msg.ts - p)
          }          
        }
        Seq(msg)
      } catch {
        case e:Exception => 
          log.warn(s"failed to parse: ${e.getMessage()}: '${data}'")
          Seq()
      }
    } else {
      log.warn(s"could not parse (not json): '${data}'")
      Seq()
    }

    msgs
  } 

  def transform(d: MeshData): Seq[MeshData] = Seq(d)  
}
