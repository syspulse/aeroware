package io.syspulse.aeroware.adsb.mesh.transport

import io.syspulse.aeroware.adsb.mesh.protocol.MSG_Options

case class MQTTConfig(
  host:String,
  port:Int = 1883,
  topic:String = "adsb-topic",
  clientId:String = "",
  protocolVer:Int = MSG_Options.V_1 | MSG_Options.O_EC
)

