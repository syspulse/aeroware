package io.syspulse.aw.gpx

import generated._
import scala.xml._

import java.time._

object App {
  def main(args:Array[String]):Unit = {
    val (gpxFile) = args match {
      case Array(f) => (f)
      case Array() => ("1.gpx")
      case _ => ("1.gpx")
    }
    val gpx = scala.io.Source.fromFile(gpxFile).getLines().mkString("\n")

    Console.err.println(gpx)

    val gpxXml = scala.xml.XML.loadString(gpx)
    val g = scalaxb.fromXML[GpxType](gpxXml)
    
    g.trk.foreach( trk => {
      Console.err.println(s"Track = ${trk.name}, ${trk.desc}\n")

      trk.trkseg.foreach( _.trkpt.foreach( trkpt => {
        val tsZ = trkpt.time.get.toGregorianCalendar().toZonedDateTime()
        val ts = tsZ.toInstant.toEpochMilli
        val tsL = LocalDateTime.ofInstant(tsZ.toInstant,ZoneId.systemDefault())
        println(s"${ts},Person-1,0001,${trkpt.lat},${trkpt.lon},${trkpt.ele.getOrElse(-1.0)},0,0,0")
      
      }))

      
    })
  }
}