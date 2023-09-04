package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.concurrent.Future
import scala.collection.immutable

import io.jvm.uuid._

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

trait DataStore {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  
  def +(m:MSG_MinerData):Future[Try[DataStore]]
  
  def ?(ts0:Long,ts1:Long):Future[Try[Seq[ValidatorData]]]
  def ??(addr:String):Future[Try[Seq[ValidatorData]]]
  
  def all:Future[Try[Seq[ValidatorData]]]

  def size:Long
}

