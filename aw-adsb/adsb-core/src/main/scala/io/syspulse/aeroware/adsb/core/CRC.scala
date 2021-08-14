package io.syspulse.aeroware.asdb.core

import scala.util.{Try,Success,Failure}

import scodec.codecs._
import scodec.bits._
import scodec.Codec

import com.typesafe.scalalogging.Logger

import io.syspulse.aeroware.asdb.core._
import io.syspulse.aeroware.util._


object CRC {
  
  def asByte(b:Boolean) = if(b) 1 else 0
  
  def calc(data:String) = calcOld(data)

  def calcOld(data:String):Boolean = {
    val gen = bin"1111111111111010000001001"
    val dataBits = BitVector.fromHex(data)

    if(!dataBits.isDefined) return false
    val msg = dataBits.get

    //println(s"gen=${gen.toHex}: ${gen.toBin}")
    //println(s"data=${data}: ${msg.toBin}")

    var m = msg
    for(i <- 0 to (msg.size - gen.size).toInt) {
      val bit = m.get(i)
      //println(s"i=${i}: bit=${asByte(bit)}: m=${m.toBin}")
      if(bit) {
        // get slice
        val slice = m.slice(i,i+gen.size)
        val xored = slice xor gen

        //println(s"i=${i}: slice=${slice.toBin}, xored=${xored.toBin}")

        // insert back by appending left and right intact parts
        val left = m.take(i)
        val right = m.drop(i + gen.size)        
        m = left ++ xored ++ right
      }
    }
    m == 0
  }
}
