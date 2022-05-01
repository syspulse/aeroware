package io.syspulse.aeroware.adsb.mesh.validator

import java.time.Duration

import com.typesafe.scalalogging.Logger

import scopt.OParser

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import akka.NotUsed
import akka.stream.scaladsl.Flow
import io.syspulse.aeroware.adsb.core.ADSB
import io.syspulse.aeroware.adsb.ADSB_Event


case class Config (
  keystoreDir:String = "",
  keystorePass:String = "",
  batchSize: Int = 3,
  batchWindow: Long = 1000L,
  ingest: io.syspulse.aeroware.adsb.ingest.Config
)

object App extends skel.Server {

  def main(args: Array[String]):Unit = {
    println(s"args: ${args.size}: ${args.toSeq}")

    val configuration = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-validator","",
        ArgString('s', "sign","Signing Key"),
        ArgString('h', "dump1090.host","Dump1090 host"),
        ArgInt('p', "dump1090.port","Dump1090 port"),
        ArgString('k', "keystore.dir","Keystore directory"),
        ArgString('r', "keystore.pass","Keystore password"),
        ArgInt('b', "batch.size","ADSB Batch max size"),
        ArgInt('w', "batch.window","ADSB Batch time window (msec)")
      )
    ))

    println(s"${configuration}")
    
    val config = Config(
      keystoreDir = configuration.getString("keystore.dir").getOrElse("./keystore/"),
      keystorePass = configuration.getString("keystore.pass").getOrElse("test123"),
      batchSize = configuration.getInt("batch.size").getOrElse(10),
      batchWindow = configuration.getLong("batch.window").getOrElse(1000L),
      ingest = io.syspulse.aeroware.adsb.ingest.Config(          
        dumpHost = configuration.getString("dump1090.host").getOrElse("rp-1"),
        dumpPort = configuration.getInt("dump1090.port").getOrElse(30002),
        fileLimit = 1000000L,
        filePattern = "NONE"
      ) 
    )

    println(config)

    new Validator(config).run()
  }
}
