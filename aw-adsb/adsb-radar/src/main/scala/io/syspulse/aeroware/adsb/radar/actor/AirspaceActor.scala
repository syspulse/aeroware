package io.syspulse.aeroware.adsb.radar.actor

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
import io.syspulse.aeroware.adsb.radar._
import io.syspulse.aeroware.core.AircraftID

object AirspaceActor {
  def apply(airspaceId: String): Behavior[Command] = Behaviors.setup(context => new AirspaceActor(context, airspaceId))

  trait Command

  private final case class AircraftTerminated(aircraft: ActorRef[AircraftActor.Command], airspaceId: String, aircraftId: String)
      extends Command

}

class AirspaceActor(context: ActorContext[AirspaceActor.Command], airspaceId: String) extends AbstractBehavior[AirspaceActor.Command](context) {
  import AirspaceActor._
  import AirspaceManager.{ AircraftRegistered, ReplyAircraftList, RequestAircraftList, RequestTrackAircraft }

  private var aircraftIdToActor = Map.empty[String, ActorRef[AircraftActor.Command]]

  context.log.info("Airspace {} started", airspaceId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackAircraft(airspaceId, aircraftId, replyTo) =>
        aircraftIdToActor.get(aircraftId) match {
          case Some(aircraftActor) =>
            replyTo ! AircraftRegistered(aircraftActor)
          case None =>
            context.log.info("Creating aircraft actor for {}", trackMsg.aircraftId)

            val aircraftActor = context.spawn(AircraftActor(airspaceId, Aircraft(AircraftID(aircraftId,aircraftId))), s"Aircraft-${aircraftId}")
            aircraftIdToActor += aircraftId -> aircraftActor

            context.watchWith(aircraftActor, AircraftTerminated(aircraftActor, airspaceId, aircraftId))
            
            replyTo ! AircraftRegistered(aircraftActor)
        }
        this

      case RequestTrackAircraft(aId, _, _) =>
        context.log.warn(s"Ignoring TrackAircraft request for ${aId}. This actor is responsible for ${airspaceId}.")
        this
      
      case RequestAircraftList(requestId, aId, replyTo) =>
        if (aId == airspaceId) {
          replyTo ! ReplyAircraftList(requestId, aircraftIdToActor.keySet)
          this
        } else
          Behaviors.unhandled

      case AircraftTerminated(_, _, aircraftId) =>
        context.log.info("AircraftActor for {} has been terminated", aircraftId)
        aircraftIdToActor -= aircraftId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("AirspaceActor {} stopped", airspaceId)
      this
  }
}