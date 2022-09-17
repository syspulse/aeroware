package io.syspulse.aeroware.adsb.mesh.validator

import java.time.Duration

import com.typesafe.scalalogging.Logger

import io.syspulse.skel
import io.syspulse.skel.config._
import io.syspulse.skel.util.Util

import akka.NotUsed
import akka.stream.scaladsl.Flow
import io.syspulse.aeroware.adsb.core.ADSB

import io.syspulse.aeroware.adsb.mesh

object AppOld extends skel.Server {

  // def main(args: Array[String]):Unit = {
  //   println(s"args: ${args.size}: ${args.toSeq}")

  //   val configuration = Configuration.withPriority(Seq(
  //     new ConfigurationAkka,
  //     new ConfigurationProp,
  //     new ConfigurationEnv, 
  //     new ConfigurationArgs(args,"adsb-validator","",
  //       ArgString('s', "sign","Signing Key"),
  //       ArgString('h', "dump1090.host","Dump1090 host"),
  //       ArgInt('p', "dump1090.port","Dump1090 port"),
  //       ArgString('k', "keystore","Keystore file (def: ./keystore/)"),
  //       ArgString('r', "keystore.pass","Keystore password"),
  //       ArgInt('b', "batch.size","ADSB Batch max size"),
  //       ArgInt('w', "batch.window","ADSB Batch time window (msec)"),
  //       ArgString('m', "mqtt.host","MQTT broker host"),
  //       ArgInt('q', "mqtt.port","MQTT borker port"),
  //     )
  //   ))

  //   println(s"${configuration}")
    
  //   val config = Config(
  //     keystore = configuration.getString("keystore").getOrElse("./keystore/"),
  //     keystorePass = configuration.getString("keystore.pass").getOrElse("abcd1234"),
  //     batchSize = configuration.getInt("batch.size").getOrElse(10),
  //     batchWindow = configuration.getLong("batch.window").getOrElse(1000L),
      
  //     mqttHost = configuration.getString("mqtt.host").getOrElse("localhost"),
  //     mqttPort = configuration.getInt("mqtt.port").getOrElse(1883),
  //   )

  //   println(config)

  //   //new Validator(config).run()
  //   new ValidatorBroker(config).run()
  // }
}
