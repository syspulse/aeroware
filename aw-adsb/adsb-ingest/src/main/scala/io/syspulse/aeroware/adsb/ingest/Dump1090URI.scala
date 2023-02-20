package io.syspulse.aeroware.adsb.ingest

/* 
dump1090://host:port
*/

case class Dump1090URI(dUri:String) {
  val PREFIX = "dump1090://"

  val (dhost:String,dport:String) = (getHost(),getPort())

  def uri:String = {
    dUri.trim.stripPrefix(PREFIX).split("[/]").toList match {
      case host :: path :: _ => PREFIX + host
      case host :: Nil => PREFIX + host
      case Nil => dUri
    }
  }

  def host:String = dhost
  def port:String = dport

  def getHost():String = {
    uri.stripPrefix(PREFIX).split("[/:]").toList match {
      case host :: port :: _ => host
      case host :: Nil => host
      case _ => ""
    }
  }

  def getPort():String = {
    uri.stripPrefix(PREFIX).split("[/:]").toList match {
      case host :: port :: _ => port
      case host :: Nil => port
      case _ => ""
    }
  }

}