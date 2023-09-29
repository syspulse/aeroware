package io.syspulse.aeroware.adsb.mesh.rewards

import scala.util.{Try,Failure,Success}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger
import scala.util.Random

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters._

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.mesh.protocol._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.crypto.key.PK

import io.syspulse.aeroware.adsb.mesh.store.DataStore

object Rewards {
  val penaltyInvalidSig = -0.5
  val penaltyNoData = -0.025            // no ADSB Data
  val penaltyMissingSomeData = -0.02    // missingsome ADSB data
  val penaltyInvalidData = -0.045       // Non-parsable ADSB data
  val penaltyTimeDiff = -0.0001          // time difference too high

  val penaltyNotPermitted = -0.0001     // Not permitted
}
