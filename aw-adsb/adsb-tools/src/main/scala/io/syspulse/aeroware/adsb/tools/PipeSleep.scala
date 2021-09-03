package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._


class PipeSleep(delay:Long) extends Pipe {
  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess)
    {
      Thread.sleep(delay)
      a
    } else 
      a
  }
}

