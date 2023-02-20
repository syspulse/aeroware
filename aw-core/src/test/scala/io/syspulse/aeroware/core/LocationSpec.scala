package io.syspulse.aeroware.core

import scala.util._

import org.scalatest.wordspec.{ AnyWordSpec}
import org.scalatest.matchers.should.{ Matchers}
import org.scalatest.flatspec.AnyFlatSpec

import java.time._
import io.jvm.uuid._
import io.syspulse.skel.util.Util

class LocationSpec extends AnyWordSpec with Matchers {

  val l0 = Location(0,0,Altitude(0,Units.METERS))

  val l1 = Location(61.7018501,7.635140,Altitude(1000.0,Units.METERS))
  val l2 = Location(61.7018502,7.635140,Altitude(1000.0,Units.METERS))

  val l3 = Location(61.701830,7.635140,Altitude(1000.0,Units.METERS))
  
  val l4 = Location(61.701,7.635,Altitude(1000.0,Units.METERS))
  val l5 = Location(61.701,7.635,Altitude(2000.0,Units.METERS))
  val l6 = Location(61.701,7.635,Altitude(1000.0,Units.FEET))

  "LocationSpec" should {
    
    s"compare selves" in {
      l0 should === (l0)
      l1 should === (l1)
      l2 should === (l2)
      l3 should === (l3)
      l4 should === (l4)
      l5 should === (l5)
    }

    s"compare ${l1} == ${l2}" in {
      l1 should === (l2)
    }

    s"NOT compare ${l1} == ${l0}" in {
      l1 should !== (l0)
    }

    s"NOT compare ${l2} == ${l3}" in {
        l2 should !== (l3)
    }

    s"NOT compare ${l4} == ${l5}" in {
        l4 should !== (l5)
    }

    s"NOT compare ${l4} == ${l6}" in {
        l4 should !== (l6)
    }

    s"compare hasCode ${l1} == ${l2}" in {
      l1.hashCode should === (l2.hashCode())
    }

    s"NOT compare hasCode ${l1} == ${l0}" in {
      l1.hashCode should !== (l0.hashCode())
    }
  }  
}
