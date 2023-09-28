package io.syspulse.aeroware.adsb.mesh

import io.syspulse.skel.util.Util

package object payload {
  type Payload = String
  type PayloadType = Int
}

object PayloadTypes {
  val ADSB = 1
  val NOTAM = 2    
}


