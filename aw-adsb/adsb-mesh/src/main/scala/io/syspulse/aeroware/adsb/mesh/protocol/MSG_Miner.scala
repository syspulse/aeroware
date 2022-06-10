package io.syspulse.aeroware.adsb.mesh.protocol

trait MSG_Miner {
  def ops:Int
}

object MSG_Options {
  val V_1 = 0x1000
  val V_2 = 0x2000
  val VER_MASK = 0xf000

  def getVer(ops:Int) = ops & VER_MASK
  def isV1(ops:Int) = getVer(V_1) != 0

  val O_BLS = 0x001
  val O_EC  = 0x000
  val SIG_MASK = 0x00f

  def getSig(ops:Int) = ops & SIG_MASK
}


