package io.syspulse.aeroware.adsb.mesh.protocol

import io.syspulse.skel.util.Util

trait MSG_Miner {
  def ops:Int
}

object MSG_Options {
  val V_1 = 0x0001    // data as hex string (to see text in console)
  val V_2 = 0x0002    // data as binary
  val VER_MASK = 0x000f

  def getVer(ops:Int) = ops & VER_MASK
  def isV1(ops:Int) = getVer(ops) == V_1
  def isV2(ops:Int) = getVer(ops) == V_2

  val O_BLS = 0x1000
  val O_EC  = 0x0000
  val SIG_MASK = 0xf000

  def getSig(ops:Int) = ops & SIG_MASK

  def fromArg(op:Option[String]) = op.map(op => {
    if(op.startsWith("0x")) BigInt(Util.fromHexString(op)).toInt else op.toInt
  })
    

  def default = MSG_Options.V_1 | MSG_Options.O_EC
  def defaultArg = "0x" + default.toHexString
}


