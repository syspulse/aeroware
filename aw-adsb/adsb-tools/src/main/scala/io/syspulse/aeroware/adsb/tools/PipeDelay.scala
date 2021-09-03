package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

// calculates how long to sleep between Events timestamp
class PipeDelay extends Pipe {
  var a0:Option[ADSB] = None

  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess) {
      
      if(a0.isDefined) {
        val a1 = a.toOption
        val elapsed = a.get.ts - a0.get.ts
        
        Thread.sleep(elapsed)

        a0 = a1
      } else {
        a0 = a.toOption
      }

      a
    } else 
      a
  }
}

