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


class MinedStoreMem extends MinedStore {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  
  val log = Logger(s"${this}")
  
  var storeAddr: mutable.Map[String,mutable.Seq[MinedData]] = mutable.HashMap()
  var storeTs: mutable.TreeMap[Long,mutable.Seq[MinedData]] = mutable.TreeMap()

  def all:Future[Try[Seq[MinedData]]] = Future{ Success(storeAddr.values.reduce(_ ++ _).toSeq) }

  def size:Long = storeAddr.values.foldLeft(0)(_ + _.size)

  def +(msg:MSG_MinerData,penalty:Double):Future[Try[MinedStore]] = Future { 
    val addr = Util.hex(msg.addr)
        
    msg.payload.foreach{ d => 
      val vd = MinedData(msg.ts,addr,d.ts,penalty,d.pt,d.data)
      log.info(s"add: ${vd}")
      
      storeAddr.getOrElseUpdate(addr, mutable.Seq()).+:(vd)
      storeTs.getOrElseUpdate(msg.ts, mutable.Seq()).+:(vd)
    }
      
    Success(this)
  }

  def ?(ts0:Long,ts1:Long):Future[Try[Seq[MinedData]]] = Future {
    Success(storeTs.range(ts0,ts1+1).values.flatten.toSeq)
  }

  def ??(addr:String):Future[Try[Seq[MinedData]]] = Future { 
    storeAddr.get(addr) match {
      case Some(dd) => Success(dd.toSeq)
      case None => Failure(new Exception(s"not found: ${addr}"))
    }
  }

}
