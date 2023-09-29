package io.syspulse.aeroware.adsb.mesh.miner

import scala.util.Random
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.{ Success,Failure}
import com.typesafe.scalalogging.Logger

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.serde.Parq._

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import io.syspulse.aeroware.adsb.mesh.PayloadTypes
import io.syspulse.aeroware.adsb.mesh.payload.PayloadType
import io.syspulse.aeroware.core.Minable
import io.syspulse.aeroware.adsb.ingest.Dump1090URI
import io.syspulse.skel.ingest.flow.Flows

// === ADSB ===================================================================

case class ADSB_Mined(adsb:ADSB) extends Minable {
  def ts = adsb.ts
  def raw = adsb.raw
}

class PipelineMinerADSB(feed:String,output:String)(implicit config:Config)
  extends PipelineMiner(feed,output) {
  
  override def getPayloadType() = PayloadTypes.ADSB

  override def decode(data:String,ts:Long):Option[Minable] = {
    Adsb.decode(data,ts) match {
      case Success(a) => Some(ADSB_Mined(a))
      case Failure(e) => None
    }
  }

  override def preparse(data:String):List[String] = data.trim.split("\\s+").toList

  override def source() = {
    feed.split("://").toList match {
      case "dump1090" :: _ => 
        val uri = Dump1090URI(feed)
        Flows.fromTcpClient(uri.host,uri.port.toInt, 
          connectTimeout = config.timeoutConnect, idleTimeout = config.timeoutIdle,
          retry = retry
        )
      case _ => super.source()
    }
  }
}


