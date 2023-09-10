package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

import akka.actor.typed.{Signal,PostStop}
import akka.actor.typed.{ActorRef,Behavior}
import akka.actor.typed.scaladsl.{ AbstractBehavior, ActorContext, Behaviors }
import akka.actor.typed.scaladsl.LoggerOps

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import io.syspulse.aeroware.core.Location
import io.syspulse.aeroware.core.SpeedType
import io.syspulse.aeroware.core.VRate
import io.syspulse.aeroware.core.Altitude
import io.syspulse.aeroware.core.Units
import io.syspulse.aeroware.core.Speed

class AirspaceActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import AircraftActor._
  import AirspaceActor._
  import AirspaceManager._

  //val a = AircraftFlying(AircraftAddress("UK-CQF-001","C-172","UR-CQF"))

  "AirspaceActorSpec" must {

    "be able to list active aircrafts" in {
      val registeredProbe = createTestProbe[AircraftRegistered]()
      val airspaceActor = spawn(AirspaceActor("AIRSPACE-1"))

      airspaceActor ! RequestTrackAircraft("AIRSPACE-1", "aircraft1", registeredProbe.ref)
      registeredProbe.receiveMessage()

      airspaceActor ! RequestTrackAircraft("AIRSPACE-1", "aircraft2", registeredProbe.ref)
      registeredProbe.receiveMessage()

      val aircraftListProbe = createTestProbe[ReplyAircraftList]()
      airspaceActor ! RequestAircraftList(requestId = 0, airspaceId = "AIRSPACE-1", aircraftListProbe.ref)
      aircraftListProbe.expectMessage(ReplyAircraftList(requestId = 0, Set("aircraft1", "aircraft2")))
    }

    "be able to list active aircrafts after one shuts down" in {
      val registeredProbe = createTestProbe[AircraftRegistered]()
      val airspaceActor = spawn(AirspaceActor("AIRSPACE-1"))

      airspaceActor ! RequestTrackAircraft("AIRSPACE-1", "aircraft1", registeredProbe.ref)
      val registered1 = registeredProbe.receiveMessage()
      val toShutDown = registered1.aircraftActor

      airspaceActor ! RequestTrackAircraft("AIRSPACE-1", "aircraft2", registeredProbe.ref)
      registeredProbe.receiveMessage()

      val aircraftListProbe = createTestProbe[ReplyAircraftList]()
      airspaceActor ! RequestAircraftList(requestId = 0, airspaceId = "AIRSPACE-1", aircraftListProbe.ref)
      aircraftListProbe.expectMessage(ReplyAircraftList(requestId = 0, Set("aircraft1", "aircraft2")))

      toShutDown ! Passivate
      registeredProbe.expectTerminated(toShutDown, registeredProbe.remainingOrDefault)

      // using awaitAssert to retry because it might take longer for the airspaceActor
      // to see the Terminated, that order is undefined
      registeredProbe.awaitAssert {
        airspaceActor ! RequestAircraftList(requestId = 1, airspaceId = "AIRSPACE-1", aircraftListProbe.ref)
        aircraftListProbe.expectMessage(ReplyAircraftList(requestId = 1, Set("aircraft2")))
      }
    }
  }
}