package io.syspulse.aeroware.core

import enumeratum._
import enumeratum.values._

import Units._

case class Location(lat:Double,lon:Double,alt:Altitude = Altitude(0.0,Units.METERS)) {
  
  def toECEF:(Double,Double,Double) = {
		val latR = scala.math.toRadians(lat)
    val lonR = scala.math.toRadians(lon)
		
		val altitude = alt.meters

		val v = Location.majorAxis / Math.sqrt(1 - Location.eccentricity2*Math.sin(latR)*Math.sin(latR))

    (
      (v + altitude) * Math.cos(latR) * Math.cos(lonR),
      (v + altitude) * Math.cos(latR) * Math.sin(lonR),
      (v * (1.0 - Location.eccentricity2) + altitude) * Math.sin(latR)
    )
  }
}

object Location {
  val majorAxis = 6378137.0; // semi-major axis
	val flattening = 1.0/298.257223563; // flattening
	val minorAxis = majorAxis*(1.0-flattening); // semi-minor axis
	val eccentricity2 = 2.0*flattening-flattening*flattening; // eccentricity squared
}
