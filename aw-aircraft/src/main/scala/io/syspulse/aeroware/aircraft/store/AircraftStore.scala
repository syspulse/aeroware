package io.syspulse.aeroware.aircraft.store

import scala.util.Try
import scala.collection.immutable

import io.jvm.uuid._

import io.syspulse.skel.store.Store
import io.syspulse.aeroware.aircraft._
import io.syspulse.aeroware.aircraft.Aircraft.ID

trait AircraftStore extends Store[Aircraft,ID] {
  def getKey(a: Aircraft): ID = Aircraft.uid(a)

  def +(a:Aircraft):Try[Aircraft]
  def del(id:ID):Try[ID]
  def ?(id:ID):Try[Aircraft]
  def all:Seq[Aircraft]
  def size:Long

  def ??(txt:String):Seq[Aircraft]

  def scan(txt:String):Seq[Aircraft]
  def search(txt:String):Seq[Aircraft]
  def grep(txt:String):Seq[Aircraft]
}
