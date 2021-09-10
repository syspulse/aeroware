package io.syspulse.aeroware.adsb.tools

import scala.util.{Try,Success,Failure}

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.core.Location
import io.syspulse.aeroware.adsb.core._


trait PipePositionBaro extends Pipe {
  var aLast:Option[ADSB_AirbornePositionBaro] = None
  var a0:Option[ADSB_AirbornePositionBaro] = None

  def process(a0:ADSB_AirbornePositionBaro,a1:ADSB_AirbornePositionBaro):Unit

  def output(a:ADSB,loc:Location) = s"${a.ts},${a.aircraftAddr.icaoId},${a.aircraftAddr.icaoCallsign},${loc.lat},${loc.lon},${loc.alt.alt}"

  def flow(a:Try[ADSB]):Try[ADSB] = {
    if(a.isSuccess && a.get.isInstanceOf[ADSB_AirbornePositionBaro]) {
      
      if(a0.isDefined) {
        val a1 = a.get.asInstanceOf[ADSB_AirbornePositionBaro]
        
        process(a0.get,a1)

        a0 = Some(a1)

      } else {
        a0 = Some(a.get.asInstanceOf[ADSB_AirbornePositionBaro])
        aLast = a0
      }

      a
    } else a
  }
}