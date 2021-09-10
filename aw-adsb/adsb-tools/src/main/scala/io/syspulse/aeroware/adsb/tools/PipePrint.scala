package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

class PipePrint extends Pipe {
  
  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess && !a.get.isInstanceOf[ADSB_Unknown]) {
      Console.println(s"${a.get}")
    }
    a
  }
}