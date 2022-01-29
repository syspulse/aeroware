package io.syspulse.aeroware.adsb.miner

import java.nio.file.{Path,Paths, Files}

import scala.util.{Try,Failure,Success}
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl.LogRotatorSink
import akka.util.ByteString
import akka.NotUsed
import akka.actor.ActorSystem

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.Await

import com.typesafe.scalalogging.Logger

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant
import java.time.format._

import upickle._
import upickle.default.{ReadWriter => RW, macroRW}

import io.syspulse.skel.ingest.IngestClient
import io.syspulse.skel.util.Util
import io.syspulse.skel.crypto.Eth
import io.syspulse.skel.crypto.wallet.WalletVaultKeyfiles

import io.syspulse.aeroware.adsb._
import io.syspulse.aeroware.adsb.core._
import io.syspulse.aeroware.adsb.core.adsb.Raw
import io.syspulse.aeroware.adsb.ingest.AdsbIngest

case class ADSB_Mined_SignedData(
  ts:Long,
  raw:Raw
)

object ADSB_Mined_SignedData {
  implicit val rw: RW[ADSB_Mined_SignedData] = macroRW
}

class ADSBMiner(config:Config) extends AdsbIngest {
  
  import MSG_Miner_Data._
  import MSG_Miner_ADSB._
 
  val wallet = new WalletVaultKeyfiles(config.keystoreDir, (keystoreFile) => {config.keystorePass})
  
  val wr = wallet.load()
  log.info(s"wallet: ${wr}")
  val signerAddr = wallet.signers.toList.head._2.head.addr

  val sinkRestartable =  { 
    RestartSink.withBackoff(retrySettings) { () =>
      Sink.foreach[MSG_Miner](m => println(s"${m}"))
    }
  }

  // val signParsedFlow = Flow[ADSB].map(a => {
  //   ADSB_Mined(
  //     a.ts,a.raw,
  //     wallet.msign(
  //       upickle.default.writeBinary(
  //         ADSB_Mined_SignedData(a.ts,a.raw)
  //       ),
  //       None, None
  //     )
  //     .headOption.getOrElse("0x0")
  //   )
  // })

    val signer = Flow[Seq[ADSB]].map( aa => { 
    val adsbData = aa.map(a => MSG_Miner_ADSB(a.ts,a.raw)).toArray
    val sigData = upickle.default.writeBinary(adsbData)
    val sig = wallet.msign(sigData,None, None).head

    val msgData = MSG_Miner_Data(
      ts = System.currentTimeMillis(),
      addr = Util.fromHexString(signerAddr),
      adsbs = adsbData,
      sig.split(":").foldLeft(Array[Byte]())(_ ++ Util.fromHexString(_))
    )

    msgData
  })

  def run() = {
    val adsbSource = flow(config.ingest)
    
    val adsbFlow = adsbSource
      .groupedWithin(config.batchSize, FiniteDuration(config.batchWindow,TimeUnit.MILLISECONDS))
      .via(signer)
      //.map(a => ByteString(a.toString))
      .log(s"output -> ")
      .toMat(sinkRestartable)(Keep.both)
      .run()

    adsbFlow
  }
    
}
