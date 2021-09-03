package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

class PipeRepeat(var count:Int) extends Pipe {
  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isFailure && a.toEither.left.get.isInstanceOf[java.util.NoSuchElementException])
    {
      count = count - 1
      if(count > 0)
        // reset pipeline to the beginning
        Success(null)
      else 
        a
    } else 
      a
  }
}
