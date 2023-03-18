package io.syspulse.aeroware.adsb.ingest.flow

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.Success
import scala.util.Failure

import com.typesafe.scalalogging.Logger

import akka.util.ByteString
import akka.http.javadsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util
import io.syspulse.skel.config._

import io.syspulse.skel.ingest._
import io.syspulse.skel.ingest.store._
import io.syspulse.skel.ingest.flow.Pipeline

import spray.json._
import DefaultJsonProtocol._
import java.util.concurrent.TimeUnit

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.ingest.Config

import io.syspulse.aeroware.adsb.ingest.AdsbJson
import io.syspulse.aeroware.adsb.ingest.AdsbIngestedJsonProtocol
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingested

import AdsbJson._
import AdsbIngestedJsonProtocol._

class PipelineDump1090(feed:String,output:String)(implicit config:Config) extends PipelineIngest[ADSB](feed,output) {
  
  def decodeDump1090(data:String):Option[ADSB] = {
    if(data.isEmpty() || (config.denoise.size > 0 && config.denoise.contains(data))) return None
    Decoder.decodeDump1090(data) match {
      case Success(a) => Some(a)
      case Failure(e) => None
    }
  }

  def decodeDump1090(data:String,ts:Long):Option[ADSB] = {
    if(data.isEmpty() || (config.denoise.size > 0 && config.denoise.contains(data))) return None
    Decoder.decode(data,ts) match {
      case Success(a) => Some(a)
      case Failure(e) => None
    }
  }

  def parseRaw(data:String):Seq[ADSB] = {
    if(data.isEmpty()) return Seq()
    try {
      val a = data.trim.split("\\s+").toList match {
        // 'timestamp adsb'  - already ingested raw with ts
        case ts :: a :: Nil => decodeDump1090(a,ts.toLong)
        // 'adsb' - directly from dump1090
        case a :: Nil =>  decodeDump1090(a)         
        case _ => {
          log.error(s"invalid format': ${data}")
          return Seq.empty
          None
        }
      }
      
      log.debug(s"adsb=${a}")
      a.toSeq
      
    } catch {
      case e:Exception => 
        log.error(s"failed to parse: '${data}'",e)
        Seq()
    }
  }

  override def parse(data:String):Seq[ADSB] = {
    log.debug(s"data=${data}")
    parseRaw(data)
  }

  def transform(a: ADSB): Seq[ADSB_Ingested] = {
    Seq(ADSB_Ingested(a,config.format))
  }
}
