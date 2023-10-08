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
import java.util.concurrent.ConcurrentHashMap

// ip anti-spam 
case class GuardSpam(expire:Long,bb:Seq[String] = Seq()) extends Guard {
  val log = Logger(this.getClass())

  // https://medium.com/@igabaydulin/java-concurrenthashmap-vs-scala-concurrent-map-e185e8a0b798
  // map to Timestamp
  val blacklist:concurrent.Map[String,Long] = new ConcurrentHashMap().asScala 
  
  bb.map(ip => {
    val now = System.currentTimeMillis()
    val ipResolved = ip match {
      case "localhost" => "127.0.0.1"
      case _ => ip
    }
    blacklist.put(ipResolved,now)
  })
  
  override def allow(socket:String):Boolean = {
    val ip = socket.split(":").head
    val ts = blacklist.get(ip)
    ts match {
      case Some(ts) =>
        val now = System.currentTimeMillis()
        if( now - ts < expire ) {
          log.warn(s"Spam: ${socket}")
          false
        } else {
          blacklist.remove(ip)
          true
        }
      case _ => 
        // allow 
        true
    }
  }

  override def permit(m:MSG_MinerData):Boolean = 
    allow(m.socket)

  def add(socket:String) = {
    val ip = socket.split(":").head
    blacklist.put(ip,System.currentTimeMillis())
  }
}

