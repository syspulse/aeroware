package io.syspulse.aeroware.adsb.mesh.rewards

import scala.util.{Try,Failure,Success}

import com.typesafe.scalalogging.Logger
import scala.util.Random

import scala.collection.concurrent
import scala.jdk.CollectionConverters._

object Rewards {
  val penaltyInvalidSig = -0.5
  val penaltyNoData = -0.025            // no ADSB Data
  val penaltyMissingSomeData = -0.02    // missingsome ADSB data
  val penaltyInvalidData = -0.045       // Non-parsable ADSB data
  val penaltyTimeDiff = -0.0001          // time difference too high

  val penaltyNotPermitted = -0.0001     // Not permitted
}
