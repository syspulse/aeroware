package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import scala.collection._
import scala.concurrent.duration.Duration
import java.io.Closeable
import io.syspulse.aeroware.adsb.core.adsb.Raw
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

class Expiry(expiryCheck:Duration, runner: ()=>Unit) extends Closeable {
  import java.util.concurrent._
  protected val expiryScheduler = new ScheduledThreadPoolExecutor(1)
  @volatile
  protected var expiryFuture: Option[ScheduledFuture[_]] = None

  // Start immediately
  start

  def start = {    
    if(expiryFuture.isDefined) expiryFuture.get.cancel(true)
    val task = new Runnable { 
      def run() = runner()
    }
    expiryFuture = Some(expiryScheduler.scheduleAtFixedRate(task, expiryCheck.toMillis, expiryCheck.toMillis, TimeUnit.MILLISECONDS))
  }

  override def close = {
    if(expiryFuture.isDefined) expiryFuture.get.cancel(true)
  }
}

class Radar(
  zoneId:String = "UTC", 
  expiryTime:Duration = FiniteDuration(1000 * 60,TimeUnit.MILLISECONDS),
  expiryCheck:Duration = FiniteDuration(1000 * 60,TimeUnit.MILLISECONDS)) extends Closeable { 
  
  val aircrafts:mutable.Map[AircraftAddress,Craft] = mutable.HashMap()
  val expirations:mutable.TreeMap[String,AircraftAddress] = mutable.TreeMap()
  val expiry = new Expiry(expiryCheck, expire)
  def expKey(a:AircraftAddress,ts:Long = now):String = s"${ts}:${a}"
  def now = System.currentTimeMillis

  val address0 = AircraftAddress("","","")

  def +(a:Craft):Craft = { aircrafts.put(a.getId,a); a}
  def find(id:AircraftAddress):Option[Craft] = aircrafts.get(id)

  def size = aircrafts.size
  def all:Iterable[Craft] = aircrafts.values

  def expire():Unit = {
    val expired = expirations.range( expKey(address0,now - expiryTime.toMillis), expKey(address0,now) ) 
    expired.foreach( e => {
      aircrafts.remove(e._2)
      expirations.remove(e._1)
    })
  } 

  def event(adsb:ADSB):Option[TrackTelemetry] = {
    val address = adsb.addr
    val t = aircrafts.get(address) match {
      case Some(aircraft) => {
        val t = aircraft.event(adsb)
        // update expiration
        expirations.remove( expKey(address))
        expirations.addOne( expKey(address) -> address)
        t
      }
      case None => {
        // not found, add new one
        val aircraft = Craft(address)
        val t = aircraft.event(adsb)
        aircrafts.put(address,aircraft)
        expirations.addOne(expKey(address) -> address)
        t
      }
    }
    
    t
  }

  def signal(ts:Long,data:Raw):Option[TrackTelemetry] = {
    Decoder.decode(data,ts) match {
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


