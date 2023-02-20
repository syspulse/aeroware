package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection

import com.typesafe.scalalogging.Logger

import io.jvm.uuid._
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
//import scala.collection.mutable.TreeMap
import scala.collection.mutable

class DataStoreMem extends DataStore {
  val log = Logger(s"${this}")
  
  var messages: mutable.TreeMap[Long,mutable.Seq[MSG_MinerData]] = mutable.TreeMap()

  def all:Seq[MSG_MinerData] = messages.values.reduce(_ ++ _).toSeq

  def size:Long = messages.values.foldLeft(0)(_ + _.size)

  def +(msg:MSG_MinerData):Try[DataStore] = { 
    messages.getOrElseUpdate(msg.ts, mutable.Seq()).+:(msg)
    log.info(s"${msg}")
    Success(this)
  }

  def ?(ts:Long):Seq[MSG_MinerData] = messages.getOrElse(ts,Seq()).toSeq

}
