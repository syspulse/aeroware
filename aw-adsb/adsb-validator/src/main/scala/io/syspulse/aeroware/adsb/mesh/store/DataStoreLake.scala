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

class LakeFileRotator(file:String,ts0:Long = 0) extends AutoCloseable {
  val log = Logger(s"${this}")

  var nextTs = ts0
  var pw:Option[ParquetWriter[RawData]] = None

  rotate()

  def isRotate() = System.currentTimeMillis() >= nextTs

  def rotate():Option[ParquetWriter[RawData]] = {
    close()

    nextTs = Util.nextTimestampFile(file)
    val f = Util.pathToFullPath(Util.toFileWithTime(file,System.currentTimeMillis()))
    
    log.info(s"writing -> ${f}")
    mkDir(f)

    pw = Some(ParquetWriter.of[RawData].build(Path(f)))
    pw
  }

  def mkDir(path:String) = {
    val baseDir = os.Path(path,os.pwd).baseName
    os.makeDir.all(os.Path(baseDir,os.pwd))
  }

  def getWriter() = pw

  def close() = {
    pw.map(pw => {
      pw.close()
    })
    pw = None
  }
}

class DataStoreLake(dir:String = "./lake/{addr}/data-{HH}{mm}/data-{id}.parq")(implicit config:Config) extends DataStore {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val log = Logger(s"${this}")

  val writerOptions = ParquetWriter.Options(
    compressionCodecName = CompressionCodecName.SNAPPY,
    writeMode = Mode.OVERWRITE,
    //hadoopConf = hadoopConf
  )

  @volatile
  var files:Map[String,LakeFileRotator] = Map()

  // shutdown hook to close unfinished files
  Runtime.getRuntime().addShutdownHook(new Thread(){
    override def run() = {
      files.values.foreach(_.close())
    }
  })

  def all:Future[Try[Seq[RawData]]] = Future{ Failure(new Exception("not supported")) }

  def size:Long = -1


  def +(msg:MSG_MinerData,penalty:Double):Future[Try[DataStore]] = {
    val addr = Util.hex(msg.addr)
        
    val vdd = msg.data.map{ d => 
      RawData(msg.ts,addr,d.ts,d.adsb,penalty)
    }

    val file = dir.replaceAll("\\{addr\\}",addr).replaceAll("\\{id\\}",config.id)

    Future {
      // find file
      val pw = files.get(addr) match {
        case Some(f) => 
          if(f.isRotate()) {
            f.rotate()
            
          } else 
            f.getWriter()
        case None => 
          val f:LakeFileRotator = new LakeFileRotator(file)          
          val pw = f.getWriter()          
          files = files + (addr -> f)   
          pw       
      }

      
      log.info(s"add: vd(${vdd.size}) -> ${pw}")  
      pw.map(_.write(vdd))
        
      Success(this)
    }
  }

  def ?(ts0:Long,ts1:Long):Future[Try[Seq[RawData]]] = Future {
    Failure(new Exception("not supported"))
  }

  def ??(addr:String):Future[Try[Seq[RawData]]] = Future {
    Failure(new Exception("not supported"))
  }

}
