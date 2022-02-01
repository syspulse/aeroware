package io.syspulse.aeroware.adsb.miner

import io.syspulse.aeroware.adsb.ADSB_Event

import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.core.adsb.Raw

//case class ADSB_Mined(adsb: ADSB, sig:String) extends ADSB_Event
case class ADSB_Mined(ts: Long, raw: Raw, sig:String) extends ADSB_Event
//case class ADSB_Mined(ts: Long, raw: String, `type`: String, icaoId:String, aircraft:String,callsign:String,sig:String) extends ADSB_Event

