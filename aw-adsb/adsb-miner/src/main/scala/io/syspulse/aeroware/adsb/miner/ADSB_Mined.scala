package io.syspulse.aeroware.adsb.miner

import io.syspulse.aeroware.adsb.ADSB_Event

import io.syspulse.skel.crypto.Eth._

case class ADSB_Mined(ts: Long, raw: String, sig:String) extends ADSB_Event
//case class ADSB_Mined(ts: Long, raw: String, `type`: String, icaoId:String, aircraft:String,callsign:String,sig:String) extends ADSB_Event