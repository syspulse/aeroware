package io.syspulse.aeroware.adsb.core

import scala.util.{Try,Success,Failure}

import scodec.codecs._
import scodec.bits._
import scodec.Codec

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.util._


object CRC {
  
  def asByte(b:Boolean) = if(b) 1 else 0
  
  def calc(data:String):Boolean = {
    val gen = bin"1111111111111010000001001"
    val dataBits = BitVector.fromHex(data)

    if(!dataBits.isDefined || dataBits.get.size < 28) return false
    val msg = dataBits.get

    var m = msg

    for(i <- 0 to (msg.size-gen.size).toInt ) {
      val bit = m.get(i)
      if(bit) {
        // get slice
        val slice = m.slice(i,i+gen.size)
        val xored = slice xor gen

        // insert back by taking left and right parts
        val left = m.take(i)
        val right = m.drop(i + gen.size)        

        m = left ++ xored ++ right
      }
    }

    //m.toHex == "0000000000000000000000000000"
    m.toByteVector.buffer.foldLeft(0)(_ + _) == 0
  }
}
