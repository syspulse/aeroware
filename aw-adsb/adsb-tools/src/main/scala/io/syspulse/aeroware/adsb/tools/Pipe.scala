package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._

trait Pipe {
  def flow(a:Try[ADSB]):Try[ADSB]
}

class PipeNone extends Pipe {
  def flow(a:Try[ADSB]):Try[ADSB] = a
}

