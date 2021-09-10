package io.syspulse.aeroware.adsb.live

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.radar._

import akka.actor.typed.{Signal,PostStop}
import akka.actor.typed.{ActorRef,Behavior}
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import akka.actor.typed.scaladsl.LoggerOps

object AircraftActor {
  
  def apply(groupId: String, aircaft: Aircraft): Behavior[Command] =
      Behaviors.setup(context => new AircraftActor(context, groupId, aircaft))

    sealed trait Command
    final case class ReadTelemetry(requestId: Long, replyTo: ActorRef[RespondTelemetry]) extends Command
    final case class RespondTelemetry(requestId: Long, value: Option[AircraftTelemetry])
    final case class WriteTelemetry(requestId: Long, value: AircraftTelemetry, replyTo: ActorRef[TelemetryAck]) extends Command
    final case class TelemetryAck(requestId: Long)
    case object Passivate extends Command
  }

  class AircraftActor(context: ActorContext[AircraftActor.Command], groupId: String, aircaft: Aircraft) extends AbstractBehavior[AircraftActor.Command](context) {
    import AircraftActor._

    context.log.info2("AircraftActor {}-{} started", groupId, aircaft)

    override def onMessage(msg: Command): Behavior[Command] = {
      msg match {
        case ReadTelemetry(id, replyTo) =>
          replyTo ! RespondTelemetry(id, aircaft.getTelemetry)
          this
        case WriteTelemetry(id, value, replyTo) =>
          context.log.info2("Write Telemerty {}: id={}", value, id)
          
          aircaft.putTelemetry(value)

          replyTo ! TelemetryAck(id)
          this
        
        case Passivate =>
          Behaviors.stopped
      }
    }

    override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
      case PostStop =>
        context.log.info2("AircraftActor {}-{} stopped", groupId, aircaft)
        this
    }
}


