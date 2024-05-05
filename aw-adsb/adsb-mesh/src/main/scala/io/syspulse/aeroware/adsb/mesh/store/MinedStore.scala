package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.concurrent.Future
import scala.collection.immutable

import io.jvm.uuid._

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

trait MinedStore {
  
  def +(m:MSG_MinerData,penalty:Double):Future[Try[MinedStore]]
  
  def ?(ts0:Long,ts1:Long):Future[Try[Seq[MinedData]]]
  def ??(addr:String):Future[Try[Seq[MinedData]]]
  
  def all:Future[Try[Seq[MinedData]]]

  def size:Long
}

