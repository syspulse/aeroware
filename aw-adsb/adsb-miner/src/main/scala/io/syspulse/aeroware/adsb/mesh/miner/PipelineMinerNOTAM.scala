package io.syspulse.aeroware.adsb.mesh.miner

import scala.util.Random
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration,FiniteDuration}
import scala.util.{ Success,Failure}
import com.typesafe.scalalogging.Logger

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.serde.Parq._

import io.syspulse.skel.ingest.flow.Flows

import io.syspulse.aeroware.core.Raw
import io.syspulse.aeroware.adsb.mesh.PayloadTypes
import io.syspulse.aeroware.adsb.mesh.payload.PayloadType
import io.syspulse.aeroware.core.Minable

import io.syspulse.aeroware.notam.NOTAM
import io.syspulse.aeroware.notam.Notam

// === NOTAM ===================================================================

case class NOTAM_Mined(notam:NOTAM,raw:Raw) extends Minable {
  val ts = System.currentTimeMillis()  
}


class PipelineMinerNOTAM(feed:String,output:String)(implicit config:Config)
  extends PipelineMiner(feed,output) {
  
  override def getPayloadType() = PayloadTypes.NOTAM

  override def decode(data:String,ts:Long):Option[Minable] = {
    Notam.decode(data) match {
      case Success(a) => Some(NOTAM_Mined(a,data))
      case Failure(e) => None
    }
  }  
}


