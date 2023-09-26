package io.syspulse.aeroware.adsb.radar

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import scala.collection._
import scala.concurrent.duration.Duration
import java.io.Closeable
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

