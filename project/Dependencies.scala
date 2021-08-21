import sbt._

object Dependencies {

    // Versions
    lazy val versionScalaLogging = "3.9.2"
    lazy val akkaVersion    = "2.6.14"
    lazy val alpakkaVersion = "3.0.3"  
    lazy val akkaHttpVersion = "10.2.1"
    lazy val akkaKafkaVersion = "2.0.3"
    lazy val kafkaAvroSerVersion = "5.4.1"
    lazy val quillVersion = "3.6.0"
    
    lazy val skelVersion = "0.0.2"
    lazy val appVersion = "0.0.1"
    lazy val jarPrefix = "server-"
    
    lazy val appDockerRoot = "/app"

    lazy val appNameAdsb = "adsb-ingest"
    lazy val appBootClassAdsb = "io.syspulse.aeroware.adsb.App"
    //lazy val mainAppClassAdsbIngest = "com.syspulse.avia.adsb.Ingest"}

    // Akka Libraries
    val libAkkaActor =      "com.typesafe.akka"           %% "akka-actor"           % akkaVersion
    val libAkkaActorTyped = "com.typesafe.akka"           %% "akka-actor-typed"     % akkaVersion
    val libAkkaCluster =    "com.typesafe.akka"           %% "akka-cluster"         % akkaVersion
    val libAkkaHttp =       "com.typesafe.akka"           %% "akka-http"            % akkaHttpVersion
    val libAkkaHttpSpray =  "com.typesafe.akka"           %% "akka-http-spray-json" % akkaHttpVersion
    val libAkkaStream =     "com.typesafe.akka"           %% "akka-stream"          % akkaVersion

    val libAlpakkaFile =    "com.lightbend.akka"          %% "akka-stream-alpakka-file" % alpakkaVersion

    val libScalaLogging =   "com.typesafe.scala-logging"  %% "scala-logging"        % "3.9.2"
    val libLogback =        "ch.qos.logback"              %  "logback-classic"      % "1.2.3"
    val libScalaTest =      "org.scalatest"               %% "scalatest"            % "3.1.2" % Test
    //val libSpecs2core =     "org.specs2"                  %% "specs2-core"          % "2.4.17"
    val libTypesafeConfig = "com.typesafe"                %  "config"               % "1.4.1"
 
    val libScopt =          "com.github.scopt"            %% "scopt"                % "4.0.0"
    val libUUID =           "io.jvm.uuid"                 %% "scala-uuid"           % "0.3.1"
    
    //val libJline =          "org.jline"                   %  "jline"                 % "3.14.1"
    //val libJson4s =         "org.json4s"                  %%  "json4s-native"        % "3.6.7"
    val libOsLib =          "com.lihaoyi"                 %% "os-lib"               % "0.7.3"
    val libFastparseLib =   "com.lihaoyi"                 %% "fastparse"            % "2.3.2"
    val libUpickle =        "com.lihaoyi"                 %% "upickle"              % "1.4.0"
    val libUjsonLib =       "com.lihaoyi"                 %% "ujson"                % "1.3.15" 

    val libPrometheusClient =   "io.prometheus"           % "simpleclient"          % "0.10.0"
    
    val libCsv =            "com.github.tototoshi"        %% "scala-csv"            % "1.3.7"

    val libScodec =         "org.scodec"                  %% "scodec-core"          % "1.11.7"

    val libSkelCore =       "io.syspulse"                 %% "skel-core"            % skelVersion
    val libSkelIngest =     "io.syspulse"                 %% "skel-ingest"          % skelVersion
    
    // Projects
    val libCommon = Seq(libScalaLogging, libLogback, libTypesafeConfig )
    val libTest = Seq(libScalaTest, libOsLib)

    val libPrometheus = Seq(libPrometheusClient)
    
    val libSkel = Seq(libSkelCore,libSkelIngest)
    val libAeroware = Seq(libScopt,libUUID)

    val libAkka = Seq(libAkkaActor,libAkkaActorTyped,libAkkaStream)

}
  