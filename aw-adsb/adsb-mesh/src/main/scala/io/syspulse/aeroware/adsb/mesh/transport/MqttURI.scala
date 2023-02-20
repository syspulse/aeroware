package io.syspulse.aeroware.adsb.mesh.transport

/* 
mqtt://host:port
*/

case class MqttURI(dUri:String) {
  val PREFIX = "mqtt://"

  val (mhost:String,mport:String) = (getHost(),getPort())

  def uri:String = {
    dUri.trim.stripPrefix(PREFIX).split("[/]").toList match {
      case host :: path :: _ => PREFIX + host
      case host :: Nil => PREFIX + host
      case Nil => dUri
    }
  }

  def host:String = mhost
  def port:String = mport

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