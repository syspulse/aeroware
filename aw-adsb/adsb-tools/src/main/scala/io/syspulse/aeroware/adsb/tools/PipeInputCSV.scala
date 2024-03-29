package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.util.Dump1090

class PipeInputCSV(inputFile:String) extends Pipe {
  val decoder = new Decoder()
  var raw:Iterator[Array[String]] = Iterator.empty
  var data:Iterator[(String,String)] = Iterator.empty

  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess && a.get.isInstanceOf[ADSB_Continue]) {
      try {
        if(! data.hasNext) {
          // reread the file stream again
          Console.err.println(s"reading: ${inputFile}")
          raw = scala.io.Source.fromFile(inputFile).getLines().filter(_.trim!="").map( s => s.split(","))
        }
        
        data = raw.flatMap( ss => {
          ss match {
          case Array(ts,adsb,_*) => Some((ts,adsb))
          case Array(ts,adsb) => Some((ts,adsb))
          case Array(adsb) => Some((System.currentTimeMillis().toString,adsb))
          case _ => Console.err.println(s"could not parse line: ${ss}"); None; // skip error line
        }}) 
      } catch {
        case e:Exception => {
          Console.err.println(s"failed to read: ${inputFile}: ${e}")
          Failure(e)
        }
      }
    }
    
    try {
      val raw = Dump1090.decode(data.next()._2)
      
      var a0 = decoder.decode(raw).get
      Success(a0)
    } catch {
      case e:java.util.NoSuchElementException => Failure(e)
      // ignore errors, just report 
      case e:Exception => {
        Console.err.println(s"failed to decode: ${e}")
        Success(ADSB_Unknown(0,0,null,""))
      }
    }
  }

}