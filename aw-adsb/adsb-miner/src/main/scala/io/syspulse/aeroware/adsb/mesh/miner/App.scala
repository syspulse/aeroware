package io.syspulse.aeroware.adsb.mesh.miner

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
  keystore:String = "",
  keystorePass:String = "",
  batchSize: Int = 3,
  batchWindow: Long = 1000L,
  ingest: io.syspulse.aeroware.adsb.ingest.Config,
  mqttHost:String = "",
  mqttPort:Int = 0
)

object App extends skel.Server {

  def main(args: Array[String]):Unit = {
    println(s"args: ${args.size}: ${args.toSeq}")

    val configuration = Configuration.withPriority(Seq(
      new ConfigurationAkka,
      new ConfigurationProp,
      new ConfigurationEnv, 
      new ConfigurationArgs(args,"adsb-miner","",
        ArgString('s', "sign","Signing Key"),
        ArgString('h', "dump1090.host","Dump1090 host"),
        ArgInt('p', "dump1090.port","Dump1090 port"),
        ArgString('k', "keystore","Keystore file (def: keystore/miner.json)"),
        ArgString('r', "keystore.pass","Keystore password"),
        ArgInt('b', "batch.size","ADSB Batch max size"),
        ArgInt('w', "batch.window","ADSB Batch time window (msec)"),
        ArgString('m', "mqtt.host","Validator MQTT broker host"),
        ArgInt('q', "mqtt.port","Validator MQTT borker port"),
      )
    ))

    println(s"${configuration}")
    
    val config = Config(
      keystore = configuration.getString("keystore").getOrElse("./keystore/miner-1.json"),
      keystorePass = configuration.getString("keystore.pass").getOrElse("test123"),
      batchSize = configuration.getInt("batch.size").getOrElse(10),
      batchWindow = configuration.getLong("batch.window").getOrElse(1000L),
      ingest = io.syspulse.aeroware.adsb.ingest.Config(          
        dumpHost = configuration.getString("dump1090.host").getOrElse("rp-1"),
        dumpPort = configuration.getInt("dump1090.port").getOrElse(30002),
        fileLimit = 1000000L,
        filePattern = "NONE"
      ),
      mqttHost = configuration.getString("mqtt.host").getOrElse("localhost"),
      mqttPort = configuration.getInt("mqtt.port").getOrElse(1883),
    )

    println(config)

    new Miner(config).run()
  }
}
