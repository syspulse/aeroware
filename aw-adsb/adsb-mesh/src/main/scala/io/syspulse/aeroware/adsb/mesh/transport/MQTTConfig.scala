package io.syspulse.aeroware.adsb.mesh.transport

case class MQTTConfig(
  host:String,
  port:Int = 1883,
  topic:String = "adsb-topic",
  clientId:String = "adsb-client",
  protocolVer:Int = 0x100
)

