package io.syspulse.aeroware.core

trait Minable extends Serializable {
  def ts:Long
  def raw: Raw
}
  
