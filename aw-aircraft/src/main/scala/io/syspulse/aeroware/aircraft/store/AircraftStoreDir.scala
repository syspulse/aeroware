package io.syspulse.aeroware.aircraft.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection.immutable

import com.typesafe.scalalogging.Logger

import os._

import spray.json._
import DefaultJsonProtocol._
import io.syspulse.skel.store.StoreDir

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.ZoneOffset
import io.syspulse.skel.util.Util


import io.syspulse.aeroware.aircraft.Aircraft
import io.syspulse.aeroware.aircraft.server.AircraftJson._
import io.syspulse.aeroware.aircraft.Aircraft.ID
import io.syspulse.aeroware.aircraft.store._

object AircraftStoreDir {
    
}

class AircraftStoreDir(dir:String = "store/") extends StoreDir[Aircraft,ID](dir) with AircraftStore {
  val store = new AircraftStoreMem

  def toKey(id:String):ID = id
  def all:Seq[Aircraft] = store.all
  def size:Long = store.size
  override def +(u:Aircraft):Try[Aircraft] = super.+(u).flatMap(_ => store.+(u))

  override def del(uid:ID):Try[ID] = super.del(uid).flatMap(_ => store.del(uid))
  override def ?(uid:ID):Try[Aircraft] = store.?(uid)

  override def ??(txt:String):Seq[Aircraft] = store.??(txt)

  // override def findByXid(xid:String):Option[Aircraft] = store.findByXid(xid)
  // override def findByEmail(email:String):Option[Aircraft] = store.findByEmail(email)
  
  override def scan(txt:String):Seq[Aircraft] = store.scan(txt)
  override def search(txt:String):Seq[Aircraft] = store.search(txt)
  override def grep(txt:String):Seq[Aircraft] = store.grep(txt)

  // preload and watch
  load(dir)
  watch(dir)
}