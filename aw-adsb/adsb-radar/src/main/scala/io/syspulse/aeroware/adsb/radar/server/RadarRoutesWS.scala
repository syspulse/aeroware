package io.syspulse.aeroware.adsb.radar.server

import scala.util.{Try,Success,Failure}
import com.typesafe.scalalogging.Logger

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.ActorContext

import akka.http.scaladsl.model.ws.Message
import akka.stream.ActorMaterializer
import akka.actor._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.service.ws.WsRoutes
import io.syspulse.skel.service.ws.WebSocket
import io.syspulse.skel.Command
import io.syspulse.aeroware.adsb.radar.store.RadarStore
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import io.syspulse.aeroware.adsb.radar.Expiry
import scala.concurrent.Await

class RadarWebSocket()(implicit ex:ExecutionContext,mat:ActorMaterializer) extends WebSocket(idleTimeout = 1000L*60*60*24) {
  override def process(m:Message,a:ActorRef):Message = {
    val txt = m.asTextMessage.getStrictText
    
    // debug
    log.info(s"${a} -> ${txt}")
    m
  }
}

class RadarRoutesWS(store: RadarStore,uri:String)(implicit context: ActorContext[_]) extends WsRoutes(uri)(context) {  
  val log = Logger(s"${this}")
  
  import spray.json._
  import RadarProto._

  val rws = new RadarWebSocket()
  override def ws:WebSocket = rws

  val signal = new Expiry(FiniteDuration(1000L,TimeUnit.MILLISECONDS),() => {
    val ts1 = System.currentTimeMillis()
    val f = store.??(ts1 - 1000L, ts1)
    //val f = store.all
    Await.result(f,FiniteDuration(3000L,TimeUnit.MILLISECONDS)) match {
      case Success(tt) => 
        if(tt.size > 0) {
          //val payload = tt.map(t => t.toJson.compactPrint).mkString("\n")
          val payload = tt.map(t => 
            s"${t.ts},${t.aid.icaoId},${t.aid.callsign},${t.loc.lat},${t.loc.lon},${t.loc.alt.alt},${t.hSpeed.v},${t.vRate.v},${t.heading}"
          ).mkString("\n")
          rws.broadcastText(payload)
        }
      case Failure(e) => 
        log.warn(s"could not retrieve telemetry: ${e}")
    }    
  })
}
