package io.syspulse.aeroware.adsb.ingest.old

import akka.NotUsed

case class Config (
  httpHost: String = "0.0.0.0",
  httpPort: Int = 8080,
  httpUri: String = "/api/v1/adsb",

  dumpHost: String = "localhost",
  dumpPort: Int = 30002,
  fileLimit: Long = 1000000L,
  fileSize: Long = 1024L * 1024L * 10L,
  filePattern: String = "ADSB-{yyyy-MM-dd'T'HH:mm:ssZ}.log",
  connectTimeout: Long = 3000L,
  idleTimeout: Long = 60000L,
  dataDir:String = "/data",
  dataFormat:String = "json",
  trackAircraft:String = "", // [Aa][nN].* - Antonov track
  args:Seq[String] = Seq()
)

