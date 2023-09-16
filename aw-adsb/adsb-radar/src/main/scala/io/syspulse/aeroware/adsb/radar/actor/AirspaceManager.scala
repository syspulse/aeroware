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

object AirspaceManager {
  def apply(): Behavior[Command] = Behaviors.setup(context => new AirspaceManager(context))

  sealed trait Command

  final case class FlyAircraft(airspaceId: String, aircraftId: String) extends AirspaceManager.Command
        with AirspaceActor.Command

  final case class RequestTrackAircraft(airspaceId: String, aircraftId: String, replyTo: ActorRef[AircraftRegistered]) extends AirspaceManager.Command
        with AirspaceActor.Command
  
  final case class AircraftRegistered(aircraftActor: ActorRef[AircraftActor.Command]) extends AirspaceManager.Command
  
  final case class RequestAircraftList(requestId: Long, airspaceId: String, replyTo: ActorRef[ReplyAircraftList]) extends AirspaceManager.Command
        with AirspaceActor.Command
  final case class ReplyAircraftList(requestId: Long, ids: Set[String])

  private final case class AirspaceTerminated(airspaceId: String) extends AirspaceManager.Command
}

class AirspaceManager(context: ActorContext[AirspaceManager.Command]) extends AbstractBehavior[AirspaceManager.Command](context) {
  import AirspaceManager._

  var airspaceIdToActor = Map.empty[String, ActorRef[AirspaceActor.Command]]

  context.log.info("AirspaceManager started")

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case FlyAircraft(airspaceId,aircraftId) =>
        context.self ! RequestTrackAircraft(airspaceId,aircraftId,context.self)
        this
      case AircraftRegistered(aircraftActor) => 
        this
      case trackMsg @ RequestTrackAircraft(airspaceId, _, replyTo) =>
        airspaceIdToActor.get(airspaceId) match {
          case Some(ref) =>
            ref ! trackMsg
          case None =>
            context.log.info(s"Creating AirspaceActor(${airspaceId})")
            val airspaceActor = context.spawn(AirspaceActor(airspaceId), "airspace-" + airspaceId)
            context.watchWith(airspaceActor, AirspaceTerminated(airspaceId))
            airspaceActor ! trackMsg
            airspaceIdToActor += airspaceId -> airspaceActor
        }
        this

      case req @ RequestAircraftList(requestId, airspaceId, replyTo) =>
        airspaceIdToActor.get(airspaceId) match {
          case Some(ref) =>
            ref ! req
          case None =>
            replyTo ! ReplyAircraftList(requestId, Set.empty)
        }
        this

      case AirspaceTerminated(airspaceId) =>
        context.log.info("AircraftActor airspace actor for {} has been terminated", airspaceId)
        airspaceIdToActor -= airspaceId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("AirspaceManager stopped")
      this
  }

}