package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._


class PipeInputADSB(inputFile:String) extends Pipe {

  val decoder = new Decoder()
  var raw:Iterator[Array[String]] = Iterator.empty
  var data:Iterator[(String,String)] = Iterator.empty

  def flow(a:Try[ADSB]):Try[ADSB] = {
    
    if(a.get == null) {
      raw = scala.io.Source.fromFile(inputFile).getLines().filter(_.trim!="").map( s => s.split("\\s+"))
      data = raw.map( ss => (ss(0),ss(1)))
    }

    try {
      var a0 = decoder.decode(data.next()._2).get.asInstanceOf[ADSB_AirbornePositionBaro]
      Success(a0)
    } catch {
      case e:Exception => Failure(e)
    }
  }

}