package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection

import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

//import scala.collection.mutable.TreeMap
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.io.OutputStreamWriter

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import io.syspulse.aeroware.adsb.mesh.validator.Config
import java.io.BufferedWriter
import java.io.FileWriter

class CsvFileRotator(file:String,ts0:Long = Long.MaxValue,flushes:Int = 0) extends AutoCloseable {
  val log = Logger(s"${this}")

  var numWrites = 0
  var nextTs = 0L
  var pw:Option[BufferedWriter] = None
  
  rotate()

  def isRotate() = System.currentTimeMillis() >= nextTs

  def rotate():Option[BufferedWriter] = {
    close()

    try {
      nextTs = Util.nextTimestampFile(file)
      if(nextTs == 0L)
        nextTs = ts0

      val f = Util.pathToFullPath(Util.toFileWithTime(file,System.currentTimeMillis()))    
    
      log.info(s"writing -> ${f}")
      mkDir(f)    

      pw = Some(new BufferedWriter(new FileWriter(f)))
      pw

    } catch {
      case e:Exception =>
        log.error(s"could not rotate ${file}",e)
        None
    }
  }

  def mkDir(path:String) = {
    val baseDir = os.Path(path,os.pwd).baseName
    os.makeDir.all(os.Path(baseDir,os.pwd))
  }
  
  def write(s:String) = {
    pw.map(pw => {
      pw.write(s)
      numWrites = numWrites + 1
      if(numWrites > flushes) {
        
        pw.flush()
        numWrites = 0
      }
    })
  }

  def close() = {
    pw.map(pw => {
      pw.close()
    })
    pw = None
  }
}

class RawStoreFile(dir:String = "./lake/{addr}/data-{yyyy}-{MM}-{dd}_{HH}:{mm}/data-{id}.csv",flushes:Int = 1)(implicit config:Config) extends RawStore {
  implicit val ec: scala.concurrent.ExecutionContext = 
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    //scala.concurrent.ExecutionContext.global

  val log = Logger(s"${this}")

  override def toString = s"RawStoreFile(${dir},${flushes})"

  @volatile
  var files:Map[String,CsvFileRotator] = Map()

  // shutdown hook to close unfinished files
  Runtime.getRuntime().addShutdownHook(new Thread(){
    override def run() = {
      files.values.foreach(_.close())
    }
  })

  def all:Future[Try[Seq[RawData]]] = Future{ Failure(new Exception("not supported")) }

  def size:Long = -1

  def +(msg:MSG_MinerData,penalty:Double):Future[Try[RawStore]] = {
    val addr = Util.hex(msg.addr)
        
    val vdd = msg.payload.map{ d => 
      RawData(msg.ts,addr,d.ts,d.pt,d.data,penalty)
    }    

    val file = dir.replaceAll("\\{addr\\}",addr).replaceAll("\\{id\\}",config.id)
    
    Future {
      // find file
      try {
        val fr = files.get(addr) match {
          case Some(fr) => 
            if(fr.isRotate()) {
              fr.rotate()              
            }
            fr
          case None => 
            val fr:CsvFileRotator = new CsvFileRotator(file,flushes = flushes)          
            files = files + (addr -> fr)
            fr
        }

        
        //log.info(s"add: vd(${vdd.size}) -> ${fr}")        
        val csv = vdd.map(Util.toCSV(_)).mkString("\n")+"\n"

        fr.write(csv)
        
        Success(this)

      } catch {
        case e:Exception => Failure(e)
      }
    }
  }

  def ?(ts0:Long,ts1:Long):Future[Try[Seq[RawData]]] = Future {
    Failure(new Exception("not supported"))
  }

  def ??(addr:String):Future[Try[Seq[RawData]]] = Future {
    Failure(new Exception("not supported"))
  }

}
