package io.syspulse.aeroware.adsb.mesh.store

import scala.util.Try
import scala.util.{Success,Failure}
import scala.collection

import com.typesafe.scalalogging.Logger

import io.jvm.uuid._

//import scala.collection.mutable.TreeMap
import scala.collection.mutable
import scala.concurrent.Future

import com.github.mjakubowski84.parquet4s.{ParquetReader, ParquetWriter, Path}
import io.syspulse.skel.serde.Parq._
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.hadoop.ParquetFileWriter.Mode

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData
import io.syspulse.aeroware.adsb.mesh.validator.Config
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

import io.syspulse.aeroware.adsb.mesh.store.RawData
import io.syspulse.aeroware.adsb.mesh.store.RawStore

class ParqFileRotator(file:String,ts0:Long = Long.MaxValue,flushes:Int = 1000) extends AutoCloseable {
  val log = Logger(s"${this}")

  var numWrites = 0
  var nextTs = 0L
  var pw:Option[ParquetWriter[RawData]] = None

  rotate()

  def isRotate() = System.currentTimeMillis() >= nextTs

  def rotate():Option[ParquetWriter[RawData]] = {
    close()

    try {
      nextTs = Util.nextTimestampFile(file)
      if(nextTs == 0L)
        nextTs = ts0

      val f = Util.toFileWithTime(file,System.currentTimeMillis())
      val dir = Util.getParentUri(f)    
      log.info(s"writing -> ${f}")
      mkDir(dir)

      pw = Some(ParquetWriter.of[RawData].build(Path(f)))
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

  def write(dd:Array[RawData]) = {
    pw.map(pw => {
      pw.write(dd)
      numWrites = numWrites + 1
      if(numWrites > flushes) {
        
        // flush is not supported
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


// Parquet does not support flush
class RawStoreParq(dir:String = "./lake/{addr}/data-{yyyy}-{MM}-{dd}_{HH}:{mm}/data-{id}.parq",flushes:Int = 1000)(implicit config:Config) extends RawStore {
  implicit val ec: scala.concurrent.ExecutionContext = 
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    //scala.concurrent.ExecutionContext.global

  val log = Logger(s"${this}")
  
  val writerOptions = ParquetWriter.Options(
    compressionCodecName = CompressionCodecName.SNAPPY,
    writeMode = Mode.OVERWRITE,
    //hadoopConf = hadoopConf
  )

  // detect single file output
  val singleFile = ! dir.contains("{addr}")

  @volatile
  var files:Map[String,ParqFileRotator] = Map()

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
        
    // find file
    val key = if(singleFile) "" else addr
    val fr = files.get(key) match {
      case Some(fr) => 
        if(fr.isRotate()) {
          fr.rotate()              
        }
        fr
      case None => 
        val file = dir.replaceAll("\\{addr\\}",addr).replaceAll("\\{id\\}",config.id)
        val fr:ParqFileRotator = new ParqFileRotator(file)          
        files = files + (key -> fr)
        fr
    }
          
    Future {
      try {
        val vdd = msg.payload.map{ d => 
          RawData(msg.ts,addr,d.ts,penalty,d.pt,d.data)
        }
      
        log.info(s"add: vd(${vdd.size}) -> ${fr}")  
        
        fr.write(vdd)
          
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
