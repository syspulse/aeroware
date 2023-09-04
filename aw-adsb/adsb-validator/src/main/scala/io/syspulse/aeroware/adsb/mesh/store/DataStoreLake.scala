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

import com.github.mjakubowski84.parquet4s.{ParquetReader, ParquetWriter, Path}
import io.syspulse.skel.serde.Parq._

class DataStoreLake(dir:String = "./lake") extends DataStore {
  val log = Logger(s"${this}")
    
  def all:Future[Try[Seq[ValidatorData]]] = Future{ Failure(new Exception("not supported")) }

  def size:Long = -1

  def +(msg:MSG_MinerData):Future[Try[DataStore]] = Future { 
    val addr = Util.hex(msg.addr)
        
    val vdd = msg.data.map{ d => 
      ValidatorData(msg.ts,addr,d.ts,d.adsb)
    }

    val file = s"${dir}/${addr}.parquet"
    log.info(s"add: vd(${vdd.size}) -> ${file}")              
    ParquetWriter.of[ValidatorData].writeAndClose(Path(file), vdd)
      
    Success(this)
  }

  def ?(ts0:Long,ts1:Long):Future[Try[Seq[ValidatorData]]] = Future {
    Failure(new Exception("not supported"))
  }

  def ??(addr:String):Future[Try[Seq[ValidatorData]]] = Future {
    Failure(new Exception("not supported"))
  }

}
