package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try

import scala.collection.immutable

import io.jvm.uuid._
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

trait DataStore {
  
  def +(m:MSG_MinerData):Try[DataStore]
  def ?(ts:Long):Seq[MSG_MinerData]
  def all:Seq[MSG_MinerData]
  def size:Long
}

