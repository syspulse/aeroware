package io.syspulse.aeroware.adsb.mesh.guard

import scala.util.{Try,Failure,Success}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger
import scala.collection.concurrent
import scala.jdk.CollectionConverters._

import io.syspulse.skel.util.Util
import io.syspulse.aeroware.adsb.mesh.protocol.MSG_MinerData

trait Guard {  
  def permit(m:MSG_MinerData):Boolean
}


class GuardEngine(guards0:List[Guard] = List()) {

  var guards:List[Guard] = guards0

  def permit(m:MSG_MinerData):Boolean = {
    // quickly find first non permit
    val failed = guards.find{ g => !g.permit(m)}
    // return only if not found
    ! failed.isDefined
  }

  def +(g:Guard):GuardEngine = {
    guards = guards :+ g
    this
  }
}
