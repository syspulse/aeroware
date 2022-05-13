package io.syspulse.aeroware.adsb.mesh.transport

case class MQTTConfig(
  host:String,
  port:Int = 1883,
  topic:String = "adsb-topic",
  clientId:String = "",
  protocolVer:Int = 0x100
)

