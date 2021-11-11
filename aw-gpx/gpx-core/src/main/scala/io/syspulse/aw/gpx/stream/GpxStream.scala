package io.syspulse.aw.gpx.stream

import generated._
import scala.xml._

import javax.xml._
import javax.xml.stream._
import javax.xml.stream.events._

import scala.jdk.CollectionConverters._

import java.io.File 
import java.time._

import xs4s._ 

class GpxStream {
  System.setProperty("javax.xml.stream.XMLInputFactory", "com.sun.xml.internal.stream.XMLInputFactoryImpl") 

  def process(gpxFile:String):Unit = {
    val xmlStream = xs4s.XMLStream.fromFile(new File(gpxFile))
    xmlStream.asScala.foreach( v => {val e= v.asInstanceOf[XMLEvent]; if(e.isStartElement) println(e.asInstanceOf[StartElement])})
  }
}