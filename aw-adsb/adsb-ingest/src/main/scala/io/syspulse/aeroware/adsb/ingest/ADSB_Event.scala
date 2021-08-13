package io.syspulse.aeroware.adsb

case class ADSB_Event(ts: Long, raw: String, `type`: String, icaoId:String, aircraft:String,callsign:String)