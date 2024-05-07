package io.syspulse.aeroware.aircraft.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection.immutable

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

import io.syspulse.aeroware.aircraft._
import io.syspulse.aeroware.aircraft.Aircraft.ID
import io.syspulse.aeroware.aircraft.icao.AircraftICAORegistry

class AircraftStoreMem extends AircraftStore {
  val log = Logger(s"${this}")
  
  val default = AircraftICAORegistry.getRegistry()

  var aircrafts: Map[ID,Aircraft] = default.map{ case(id,a) => 
    id -> Aircraft(
      id = a.icao,
      rid = a.regid,
      model = a.mdl,
      typ = a.icaoType,
      call = None,
      ts = 0L
    )  
  }

  def all:Seq[Aircraft] = aircrafts.values.toSeq

  def size:Long = aircrafts.size

  def +(a:Aircraft):Try[Aircraft] = { 
    aircrafts = aircrafts + (Aircraft.uid(a) -> a)
    log.info(s"${Aircraft}")
    Success(a)
  }

  def del(id:ID):Try[ID] = { 
    val sz = aircrafts.size
    aircrafts = aircrafts - id;
    log.info(s"${id}")
    if(sz == aircrafts.size) Failure(new Exception(s"not found: ${id}")) else Success(id)  
  }

  def ?(id:ID):Try[Aircraft] = aircrafts.get(id) match {
    case Some(y) => Success(y)
    case None => Failure(new Exception(s"not found: ${id}"))
  }

  def ??(txt:String,from:Long,size:Long):Seq[Aircraft] = {
    val pattern = s"(?i)${txt}"

    val rr = aircrafts.values.filter(a => 
      a.id.matches(pattern) || 
      a.rid.matches(pattern) ||       
      a.model.matches(pattern) ||
      a.call.map(_.matches(pattern)).getOrElse(false)
    ).toSeq

    if(from == 0L && size == Long.MaxValue)
      rr
    else
      rr.drop(from.toInt).take(size.toInt)
  }
  
  def search(txt:String,from:Long,size:Long):Seq[Aircraft] = ??(txt,from,size)

  def grep(txt:String):Seq[Aircraft] = search(txt)
  
  def typing(txt:String,size:Int):Seq[Aircraft] = ??(txt + ".*",0,size)
}
