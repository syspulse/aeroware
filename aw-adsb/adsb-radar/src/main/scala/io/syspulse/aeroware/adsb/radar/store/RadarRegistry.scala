package io.syspulse.aeroware.adsb.radar.store

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

import io.syspulse.skel.Command

import scala.util.Try
import io.syspulse.aeroware.adsb.radar.server.RadarTelemetry
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

object RadarRegistry {
  val log = Logger(s"${this}")

  implicit val ec: scala.concurrent.ExecutionContext = 
    scala.concurrent.ExecutionContext.global
    //ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))
  
  final case class GetRadarTelemetry(replyTo: ActorRef[Try[RadarTelemetry]]) extends Command
  final case class GetRadarTelemetryTime(aid:String,ts0:Long,ts1:Long,replyTo: ActorRef[Try[RadarTelemetry]]) extends Command
  
  // this var reference is unfortunately needed for Metrics access
  var store: RadarStore = null 

  def apply(store: RadarStore): Behavior[io.syspulse.skel.Command] = {
    this.store = store
    registry(store)
  }

  private def registry(store: RadarStore): Behavior[io.syspulse.skel.Command] = {
    this.store = store

    Behaviors.receiveMessage {
      case GetRadarTelemetry(replyTo) =>
        val f = for {
          rt <- store.all            
          r <- {
            Future(
            rt match {
              case Success(tt) => 
                Success(RadarTelemetry(tt,total = Some(tt.size)))
              case Failure(e) => Failure(e)              
            }            
          )
          }          
        } yield r
        
        f.map(rsp => replyTo ! rsp)

        // //replyTo ! rsp
        // val f = Future{ Success(RadarTelemetry(Seq(),total = Some(10))) }
        // f.map(rsp => replyTo ! rsp)        

        Behaviors.same
      
      case GetRadarTelemetryTime(aid, ts0, ts1, replyTo) =>
        //replyTo ! Telemetrys(store.?(id,ts0,ts1))
        Behaviors.same
      
    }
  }
}
