package io.syspulse.aeroware.adsb.radar.actor

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

import io.syspulse.aeroware.adsb.radar._

class AircraftActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import AircraftActor._

  "AircraftActor" must {

    "reply with empty telemetry" in {
      val probe = createTestProbe[RespondTelemetry]()
      val aa = spawn(AircraftActor("group", Trackable.Aircraft(AircraftAddress("UK-CQF-001","C-172","UR-CQF").toId())))

      aa ! AircraftActor.ReadTelemetry(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId should ===(42)
      response.value should ===(None)
    }

    "reply with latest Telemetry" in {
      val recordProbe = createTestProbe[TelemetryAck]()
      val readProbe = createTestProbe[RespondTelemetry]()

      val a1 = Trackable.Aircraft(AircraftAddress("UK-CQF-001","C-172","UR-CQF").toId())
      val at1 = TrackTelemetry(0L,a1.aid,loc=Location(1,1,Altitude(100,Units.METERS)),hSpeed=Speed(60,Units.KNOTS),vRate=VRate(5,Units.FPM),heading=0.0)
      val at2 = TrackTelemetry(0L,a1.aid,loc=Location(2,2,Altitude(200,Units.METERS)),hSpeed=Speed(70,Units.KNOTS),vRate=VRate(8,Units.FPM),heading=0.0)
      val aa = spawn(AircraftActor("group", a1))
      
      aa ! AircraftActor.WriteTelemetry(requestId = 1, at1, recordProbe.ref)
      recordProbe.expectMessage(AircraftActor.TelemetryAck(requestId = 1))

      aa ! AircraftActor.ReadTelemetry(requestId = 2, readProbe.ref)
      val response1 = readProbe.receiveMessage()
      response1.requestId should ===(2)
      response1.value should ===(Some(at1))

      aa ! AircraftActor.WriteTelemetry(requestId = 3, at2, recordProbe.ref)
      recordProbe.expectMessage(AircraftActor.TelemetryAck(requestId = 3))

      aa ! AircraftActor.ReadTelemetry(requestId = 4, readProbe.ref)
      val response2 = readProbe.receiveMessage()
      response2.requestId should ===(4)
      response2.value should ===(Some(at2))
    }
  }
}