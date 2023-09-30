package io.syspulse.aeroware.adsb.mesh.validator

import scala.util.{Try,Failure,Success}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger
import scala.util.Random

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext

import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles
import io.syspulse.skel.crypto.key.PK

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.mesh.protocol._
import io.syspulse.aeroware.adsb.mesh.rewards._

case class ValidatorConfig(
  validateTs:Boolean = false,
  validateSig:Boolean = true,
  validateData:Boolean = true,    // data is valid (parsing, heavy operation)
  validatePayload:Boolean = true, // data is present
  
  toleranceTs:Long = 750L,

  validateAddrBlacklist:Boolean = true,   // validate permit on Address Blacklist
  blacklistAddr:Seq[String] = Seq(),
  validateIpBlacklist:Boolean = true,     // validate permit on IP Blacklist
  blacklistIp:Seq[String] = Seq(),
)