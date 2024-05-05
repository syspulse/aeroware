package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import scala.collection._
import scala.concurrent.duration.Duration
import java.io.Closeable
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

import io.syspulse.aeroware.core.Raw
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.core.AircraftID

class Radar(
  zoneId:String = "UTC", 
  expiryTime:Duration = FiniteDuration(1000 * 60,TimeUnit.MILLISECONDS),
  expiryCheck:Duration = FiniteDuration(1000 * 60,TimeUnit.MILLISECONDS)) extends Closeable { 
  
  val aircrafts:mutable.Map[AircraftID,Trackable] = mutable.HashMap()
  val expirations:mutable.TreeMap[String,AircraftID] = mutable.TreeMap()
  val expiry = new Expiry(expiryCheck, expire)
  def expKey(a:AircraftID,ts:Long = now):String = s"${ts}:${a}"
  def now = System.currentTimeMillis

  val aid0 = AircraftID()

  def +(a:Trackable):Trackable = { aircrafts.put(a.getId(),a); a}
  def find(addr:AircraftAddress):Option[Trackable] = aircrafts.get(addr.toId())

  def size = aircrafts.size
  def all:Iterable[Trackable] = aircrafts.values

  def expire():Unit = {
    val expired = expirations.range( expKey(aid0,now - expiryTime.toMillis), expKey(aid0,now) ) 
    expired.foreach( e => {
      aircrafts.remove(e._2)
      expirations.remove(e._1)
    })
  } 

  def event(adsb:ADSB):Option[TrackTelemetry] = {
    val aid = adsb.addr.toId()
    val t = aircrafts.get(aid) match {
      case Some(aircraft) => {
        val t = aircraft.event(adsb)
        // update expiration
        expirations.remove( expKey(aid))
        expirations.addOne( expKey(aid) -> aid)
        t
      }
      case None => {
        // not found, add new one
        val aircraft = Trackable(aid)
        val t = aircraft.event(adsb)
        aircrafts.put(aid,aircraft)
        expirations.addOne(expKey(aid) -> aid)
        t
      }
    }
    
    t
  }

  def signal(ts:Long,data:Raw):Option[TrackTelemetry] = {
    Adsb.decode(data,ts) match {
      case Success(adsb) => 
        event(adsb)

      case Failure(e) =>
        None
    }
  }

  def stop = expiry.close

  override def close = { stop }

  override def toString = s"${zoneId}: ${aircrafts}, expirations=(${expirations.size})"
}


