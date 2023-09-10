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
import java.nio.ByteBuffer

// 0x address blacklist
case class GuardBlacklistAddr(bb:Seq[String]) extends Guard {
  val log = Logger(this.getClass())

  val blacklist:Map[ByteBuffer,Boolean] = bb.map(a => ByteBuffer.wrap(Util.fromHexString(a)) -> true).toMap
  
  def permit(m:MSG_MinerData):Boolean = {
    val b = blacklist.get(ByteBuffer.wrap(m.addr)).isDefined
    if(b) {
      log.debug(s"Blacklist: ${Util.hex(m.addr)}")
    }
    ! b
  }
}

// ip address blacklist
case class GuardBlacklistIp(bb:Seq[String]) extends Guard {
  val log = Logger(this.getClass())

  val blacklist:Map[String,Boolean] = bb.map(ip => {
    val ipResolved = ip match {
      case "localhost" => "127.0.0.1"
      case _ => ip
    }
    ipResolved -> true
  }).toMap
  
  def permit(m:MSG_MinerData):Boolean = {
    val ip = m.socket.split(":").head
    val b = blacklist.get(ip).isDefined
    if(b) {
      log.debug(s"Blacklist: ${m.socket}")
    }
    ! b
  }
}

