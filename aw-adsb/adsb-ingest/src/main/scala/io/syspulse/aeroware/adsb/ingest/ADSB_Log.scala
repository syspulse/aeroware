package io.syspulse.aeroware.adsb

case class ADSB_Log(ts: Long, raw: String, `type`: String, icaoId:String, aircraft:String,callsign:String) extends ADSB_Event