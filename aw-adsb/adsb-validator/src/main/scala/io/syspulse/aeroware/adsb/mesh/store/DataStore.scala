package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.concurrent.Future
import scala.collection.immutable

import io.jvm.uuid._

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

trait DataStore {
  
  def +(m:MSG_MinerData,penalty:Double):Future[Try[DataStore]]
  
  def ?(ts0:Long,ts1:Long):Future[Try[Seq[RawData]]]
  def ??(addr:String):Future[Try[Seq[RawData]]]
  
  def all:Future[Try[Seq[RawData]]]

  def size:Long
}

