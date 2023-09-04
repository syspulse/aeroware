package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection

import com.typesafe.scalalogging.Logger

import io.jvm.uuid._
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
//import scala.collection.mutable.TreeMap
import scala.collection.mutable
import scala.concurrent.Future
import io.syspulse.skel.util.Util

class DataStoreMem extends DataStore {
  val log = Logger(s"${this}")
  
  var storeAddr: mutable.Map[String,mutable.Seq[ValidatorData]] = mutable.HashMap()
  var storeTs: mutable.TreeMap[Long,mutable.Seq[ValidatorData]] = mutable.TreeMap()

  def all:Future[Seq[ValidatorData]] = Future{ storeAddr.values.reduce(_ ++ _).toSeq }

  def size:Long = storeAddr.values.foldLeft(0)(_ + _.size)

  def +(msg:MSG_MinerData):Future[Try[DataStore]] = Future { 
    val addr = Util.hex(msg.addr)
        
    msg.data.foreach{ d => 
      val vd = ValidatorData(msg.ts,addr,d.ts,d.adsb)
      log.info(s"add: ${vd}")
      
      storeAddr.getOrElseUpdate(addr, mutable.Seq()).+:(vd)
      storeTs.getOrElseUpdate(msg.ts, mutable.Seq()).+:(vd)
    }
      
    Success(this)
  }

  def ?(ts0:Long,ts1:Long):Future[Seq[ValidatorData]] = Future {
    storeTs.range(ts0,ts1+1).values.flatten.toSeq
  }

  def ??(addr:String):Future[Seq[ValidatorData]] = Future {
    storeAddr.get(addr) match {
      case Some(dd) => dd.toSeq
      case None => Seq()
    }
  }

}
