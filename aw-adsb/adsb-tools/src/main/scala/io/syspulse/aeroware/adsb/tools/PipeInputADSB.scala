package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.util.Dump1090


class PipeInputADSB(inputFile:String) extends Pipe {

  val decoder = new Decoder()
  var raw:Iterator[Array[String]] = Iterator.empty
  var data:Iterator[(String,String)] = Iterator.empty

  def flow(a:Try[ADSB]):Try[ADSB] = {
    
    if(a.get == null) {
      try {
        // try to understand the format
        // [ts,adsb]
        // [adsb]
        raw = scala.io.Source.fromFile(inputFile).getLines().filter(_.trim!="").map( s => s.split("\\s+"))
        data = raw.flatMap( ss => ss match {
          case Array(ts,adsb) => Some((ts,adsb))
          case Array(adsb) => Some((System.currentTimeMillis().toString,adsb))
          case _ => Console.err.println(s"could not parse line: ${ss}"); None; // skip error line
        })
      } catch {
        case e:Exception => {
          Console.err.println(s"failed to read: ${inputFile}: ${e}")
          Failure(e)
        }
      }
    }

    try {
      var a0 = decoder.decode(Dump1090.decode(data.next()._2)).get
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