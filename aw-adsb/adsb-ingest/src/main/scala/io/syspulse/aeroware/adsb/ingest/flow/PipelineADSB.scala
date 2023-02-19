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

class PipelineADSB(feed:String,output:String)(implicit config:Config) extends PipelineIngest[ADSB](feed,output) {
  
  def decode(data:String,ts:Long):Option[ADSB] = {
    Decoder.decode(data,ts) match {
      case Success(a) => Some(a)
      case Failure(e) => None
    }
  }

  // expect format 'timestamp adsb'
  def parse(data:String):Seq[ADSB] = {
    if(data.isEmpty()) return Seq()
    try {

      val a = data.trim.split("\\s+").toList match {
        case ts :: a :: Nil => decode(a,ts.toLong)
        case _ => {
          log.error(s"failed to parse: invalid format (expected: 'Timestamp ADSB': ${data}")
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

  def transform(a: ADSB): Seq[ADSB_Ingested] = {
    Seq(ADSB_Ingested(a,config.format))
  }
}
