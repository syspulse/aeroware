package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import scala.collection._
import scala.concurrent.duration.Duration
import java.io.Closeable

class Expiry(expiryCheck:Long = 3, runner: ()=>Unit) extends Closeable {
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
    expiryFuture = Some(expiryScheduler.scheduleAtFixedRate(task, expiryCheck, expiryCheck, TimeUnit.SECONDS))
  }

  override def close = {
    if(expiryFuture.isDefined) expiryFuture.get.cancel(true)
  }
}

class Radar(zoneId:String = "ZONE", expiryTime:Duration = Duration("1 minutes"),expiryCheck:Duration = Duration("1 minute")) extends Closeable { 
  
  val aircrafts:mutable.Map[AircraftAddress,Aircraft] = mutable.HashMap()
  val expirations:mutable.TreeMap[String,AircraftAddress] = mutable.TreeMap()
  val expiry = new Expiry(expiryCheck.toSeconds, expire)
  def expKey(a:AircraftAddress,ts:Long = now):String = s"${ts}:${a}"
  def now = System.currentTimeMillis

  val address0 = AircraftAddress("","","")

  def +(a:Aircraft):Aircraft = { aircrafts.put(a.getId,a); a}
  def find(id:AircraftAddress):Option[Aircraft] = aircrafts.get(id)

  def size = aircrafts.size
  def all:Iterable[Aircraft] = aircrafts.values

  def expire():Unit = {
    val expired = expirations.range( expKey(address0,now - expiryTime.toMillis), expKey(address0,now) ) 
    expired.foreach( e => {
      aircrafts.remove(e._2)
      expirations.remove(e._1)
    })
  } 

  def event(adsb:ADSB):Radar = {
    val address = adsb.addr
    aircrafts.get(address) match {
      case Some(aircraft) => {
        aircraft.event(adsb)
        // update expiration
        expirations.remove( expKey(address))
        expirations.addOne( expKey(address) -> address)
      }
      case None => {
        // not found, add new one
        val aircraft = Aircraft(address)
        aircraft.event(adsb)
        aircrafts.put(address,aircraft)
        expirations.addOne(expKey(address) -> address)
      }
    }
    this
  }

  def stop = expiry.close

  override def close = { stop }

  override def toString = s"${zoneId}: ${aircrafts}, expirations=(${expirations.size})"
}


