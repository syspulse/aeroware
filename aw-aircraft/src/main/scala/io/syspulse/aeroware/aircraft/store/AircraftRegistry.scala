package io.syspulse.aeroware.aircraft.store

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

import io.syspulse.skel.Command

import io.syspulse.aeroware.aircraft._
import io.syspulse.aeroware.aircraft.Aircraft.ID
import scala.util.Try

object AircraftRegistry {
  val log = Logger(s"${this}")
  
  final case class GetAircrafts(replyTo: ActorRef[Aircrafts]) extends Command
  final case class GetAircraft(id:ID,replyTo: ActorRef[Try[Aircraft]]) extends Command
  final case class SearchAircraft(txt:String,replyTo: ActorRef[Aircrafts]) extends Command
  final case class CreateAircraft(req: AircraftCreateReq, replyTo: ActorRef[Aircraft]) extends Command  
  final case class DeleteAircraft(id: ID, replyTo: ActorRef[AircraftRes]) extends Command
  
  // this var reference is unfortunately needed for Metrics access
  var store: AircraftStore = null //new AircraftStoreDB //new AircraftStoreCache

  def apply(store: AircraftStore = new AircraftStoreMem): Behavior[io.syspulse.skel.Command] = {
    this.store = store
    registry(store)
  }

  private def registry(store: AircraftStore): Behavior[io.syspulse.skel.Command] = {
    this.store = store

    Behaviors.receiveMessage {
      case GetAircrafts(replyTo) =>
        replyTo ! Aircrafts(store.all,Some(store.size))
        Behaviors.same

      case GetAircraft(id, replyTo) =>
        replyTo ! store.?(id)
        Behaviors.same

      case SearchAircraft(txt, replyTo) =>
        val ss = store.search(txt)
        replyTo ! Aircrafts(ss,Some(ss.size))
        Behaviors.same

      case CreateAircraft(req, replyTo) =>
        val a = Aircraft(
          id = req.id,
          rid = req.rid,
          model = req.model,
          typ = req.typ,
          call = req.call,
          ts = System.currentTimeMillis,
        )        
        val store1 = store.+(a)
        replyTo ! a
        Behaviors.same

      // case UpdateAircraft(replyTo) =>       
      //   Behaviors.same
      
      case DeleteAircraft(id, replyTo) =>
        val store1 = store.del(id)
        replyTo ! AircraftRes(s"deleted",Some(id))
        Behaviors.same
    }
  }
}
