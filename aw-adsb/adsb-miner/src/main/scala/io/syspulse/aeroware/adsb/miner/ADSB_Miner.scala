package io.syspulse.aeroware.adsb.miner

import java.nio.file.{Path,Paths, Files}

import scala.util.{Try,Failure,Success}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl.LogRotatorSink

import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format._

import java.net.InetSocketAddress

import scopt.OParser

import upickle._

import io.prometheus.client.Counter

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._

import akka.NotUsed
import io.syspulse.aeroware.adsb.ingest.ADSB_Ingest

class ADSB_Miner(config:Config) extends ADSB_Ingest {
  
    implicit val adsbRW = upickle.default.macroRW[ADSB_Mined]

    val wallet = new WalletVaultKeyfiles(config.keystoreDir, (keystoreFile) => {config.keystorePass})

    val sinkRestartable =  { 
      RestartSink.withBackoff(retrySettings) { () =>
        Sink.foreach[ADSB_Mined](m => println(s"${m}"))
      }
    }

    val signRawFlow = Flow[ADSB].map(a => {
      ADSB_Mined(a.ts,a.raw,Util.hex(Util.SHA256(a.raw)))
    })

    val signLogFlow = Flow[ADSB_Log].map(a => {
      //ADSB_Mined(a.ts,a.raw,Util.hex(Util.SHA256(a.raw)))
      val data = s"${a.ts},${a.raw}"
      ADSB_Mined(a.ts,a.raw,Eth.sign(data,"0x1"))
    })

    def run() = {
      val adsbSource = flow(config.ingest)
      
      val adsbFlow = adsbSource
        .via(logFlow)
        .via(signLogFlow)
        //.map(a => ByteString(a.toString))
        .log(s"output -> ")
        .toMat(sinkRestartable)(Keep.both)
        .run()

      adsbFlow
    }
    
}
