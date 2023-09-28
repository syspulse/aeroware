package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.radar._
import io.syspulse.aeroware.adsb.core._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class PipeRadarWS(wsHost:String,wsPort:Int,interval:Long=1000L,expiryTime:Long = 60L * 1) extends PipeWS(wsHost,wsPort) with Pipe  {
  val radar = new Radar(expiryTime = Duration(expiryTime,TimeUnit.SECONDS))
  var ts0 = System.currentTimeMillis()
  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess && !a.get.isInstanceOf[ADSB_Unknown]) {
      
      radar.event(a.get)

      val ts1 = System.currentTimeMillis()
      if((ts1 - ts0) > interval) {
        for(aircraft <- radar.aircrafts.values) {
          val telemetry = aircraft.last 
          if(telemetry.isDefined) {
            val t = telemetry.get
            broadcast(s"${t.ts},${t.aid.icaoId},${t.aid.callsign},${t.loc.lat},${t.loc.lon},${t.loc.alt.alt},${t.hSpeed.v},${t.vRate.v},${t.heading}")
          }
        }
        
        ts0 = ts1
      }
      
    }
    a
  }
}