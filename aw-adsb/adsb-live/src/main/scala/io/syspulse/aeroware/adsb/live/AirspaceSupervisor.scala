package io.syspulse.aeroware.adsb.live

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

import io.syspulse.aeroware.core.{ Location, Speed, Altitude, VRate, Units }
import io.syspulse.aeroware.adsb.core.{ AircraftAddress }

object AirspaceSupervisor {
  def apply(): Behavior[String] =
    Behaviors.setup[String](context => new AirspaceSupervisor(context))
}

class AirspaceSupervisor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  context.log.info("AirspaceSupervisor started")
  
  @volatile
  var airspaceManagerRef:Option[ActorRef[AirspaceManager.Command]] = None

  override def onMessage(msg: String): Behavior[String] = msg match {
    case "random" => {
      val aid = AircraftAddress("UK-CQF-001","C-172","UR-CQF")
      println(s"Aircraft: $aid")
      //val at1 = AircraftTelemetry(aid,loc=Location(1,1,Altitude(100,Units.METERS)),hSpeed=Speed(60,Units.KNOTS),vRate=VRate(5,Units.FPM))
      //actorRef ! AircraftActor.WriteTelemetry(requestId = 1, at1, context.self)

      airspaceManagerRef.get ! AirspaceManager.FlyAircraft("AIRSPACE-1",aid.icaoId)
      this
    }

    case "start" => {
      val actorRef = context.spawn(AirspaceManager(),"AirspaceManager-0")
      println(s"actorRef: ${actorRef}")
      airspaceManagerRef = Some(actorRef)
      this
    }
    // case _ =>
    //   Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      context.log.info("AirspaceSupervisor stopped")
      this
  }
}