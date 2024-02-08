import sbt._

object Dependencies {

    // Versions
    lazy val versionScalaLogging = "3.9.2"
    lazy val akkaVersion    = "2.6.20"
    lazy val alpakkaVersion = "3.0.4"  
    lazy val akkaHttpVersion = "10.2.9" //"10.2.4"
    lazy val akkaKafkaVersion = "2.0.3"
    lazy val kafkaAvroSerVersion = "5.4.1"

    // 3.12.0 - Postgres JAsync does not support Postgres 14 
    lazy val quillVersion = "3.19.0" //"4.8.0" //"3.12.0" //"3.5.2" //"3.6.0"

    lazy val influxDBVersion = "3.2.0"
    lazy val slickVersion = "3.3.3"    
    lazy val janinoVersion = "3.0.16" //"3.1.6" //"3.0.16"
    lazy val elastic4sVersion = "7.17.3"

    lazy val sparkVersion = "3.2.2" //"3.2.0"
    lazy val hadoopAWSVersion = "3.2.2"
    lazy val hadoopVersion = "3.2.2"
    lazy val parq4sVersion = "2.10.0"

    lazy val dispatchVersion = "1.2.0" //"1.1.3"
    
    lazy val skelVersion = "0.0.8"
    lazy val awVersion = "0.0.4"
    lazy val jarPrefix = "server-"
    
    lazy val appDockerRoot = "/app"

    lazy val appNameAdsbIngest              = "adsb-ingest"
    lazy val appBootClassAdsbIngest         = "io.syspulse.aeroware.adsb.ingest.App"

    lazy val appNameAdsbMiner               = "adsb-miner"
    lazy val appBootClassAdsbMiner          = "io.syspulse.aeroware.adsb.miner.App"

    lazy val appNameAdsbValidator           = "adsb-validator"
    lazy val appBootClassAdsbValidator      = "io.syspulse.aeroware.adsb.validator.App"

    //lazy val mainAppClassAdsbIngest = "com.syspulse.avia.adsb.Ingest"}

    //val scalaJava8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"

    // Akka Libraries
    val libAkkaActor =      "com.typesafe.akka"           %% "akka-actor"           % akkaVersion
    val libAkkaActorTyped = "com.typesafe.akka"           %% "akka-actor-typed"     % akkaVersion
    val libAkkaCluster =    "com.typesafe.akka"           %% "akka-cluster"         % akkaVersion
    val libAkkaHttp =       "com.typesafe.akka"           %% "akka-http"            % akkaHttpVersion
    val libAkkaHttpSpray =  "com.typesafe.akka"           %% "akka-http-spray-json" % akkaHttpVersion
    val libAkkaStream =     "com.typesafe.akka"           %% "akka-stream"          % akkaVersion
    val libAkkaStreamTyped ="com.typesafe.akka"           %% "akka-stream-typed"    % akkaVersion

    val libAlpakkaFile =    "com.lightbend.akka"          %% "akka-stream-alpakka-file" % alpakkaVersion
    val libAlpakkaMQTT=     "com.lightbend.akka"          %% "akka-stream-alpakka-mqtt-streaming" % alpakkaVersion
    val libAlpakkaPaho=     "com.lightbend.akka"          %% "akka-stream-alpakka-mqtt" % alpakkaVersion

    val libScalaLogging =   "com.typesafe.scala-logging"      %% "scala-logging"        % "3.9.2"
    val libLogback =        "ch.qos.logback"                  % "logback-classic"      % "1.3.5" //"1.2.8"
    val libJanino =         "org.codehaus.janino"             % "janino"               % janinoVersion
    // I need this rubbish slf4j to deal with old jboss dependecny which generates exception in loading logback.xml
    //val libSlf4jApi =       "org.slf4j"                   %  "slf4j-api"            % "1.8.0-beta4"
    // Supports only old XML Config file format
    // val libSlf4jApi =       "org.slf4j"                       % "slf4j-api"            % "1.7.26"
    val libSlf4jApi =       "org.slf4j"                       % "slf4j-api"            % "2.0.5"
    // Needed for teku
    val libLog4j2Api =      "org.apache.logging.log4j"        % "log4j-api" % "2.17.2"
    val libLog4j2Core =     "org.apache.logging.log4j"        % "log4j-core" % "2.17.2"

    val libQuill =          "io.getquill"                     %% "quill-jdbc"             % quillVersion
    // val libQuillAsyncPostgres =  "io.getquill"                %% "quill-async-postgres"   % quillVersion
    // val libQuillAsyncMySQL =     "io.getquill"                %% "quill-async-mysql"      % quillVersion
    val libQuillAsyncPostgres =  "io.getquill"                %% "quill-jasync-postgres"   % quillVersion
    val libQuillAsyncMySQL =     "io.getquill"                %% "quill-jasync-mysql"      % quillVersion

    val libMySQL =          "mysql"                           % "mysql-connector-java"    % "8.0.22"
    val libPostgres =       "org.postgresql"                  % "postgresql"              % "42.3.5"

    val libScalaTest =      "org.scalatest"               %% "scalatest"            % "3.1.2" % Test
    val libAkkaTestkit =    "com.typesafe.akka"           %% "akka-http-testkit"    % akkaHttpVersion// % Test
    val libAkkaTestkitType ="com.typesafe.akka"           %% "akka-actor-testkit-typed" % akkaVersion// % Test
    //val libSpecs2core =     "org.specs2"                  %% "specs2-core"          % "2.4.17"
    val libTypesafeConfig = "com.typesafe"                %  "config"               % "1.4.1"

    val libAkkaHttpCors =   "ch.megard"                       %% "akka-http-cors"       % "1.1.3"
    val libWsRsJakarta =    "jakarta.ws.rs"                   % "jakarta.ws.rs-api"     % "3.1.0" //"3.0.0"
    val libSwaggerAkkaHttp ="com.github.swagger-akka-http"    %% "swagger-akka-http"    % "2.7.0" //"2.10.0"
 
    val libScopt =          "com.github.scopt"            %% "scopt"                % "4.0.0"
    val libUUID =           "io.jvm.uuid"                 %% "scala-uuid"           % "0.3.1"
    
    //val libJline =          "org.jline"                   %  "jline"                 % "3.14.1"
    //val libJson4s =         "org.json4s"                  %%  "json4s-native"        % "3.6.7"
    val libOsLib =          "com.lihaoyi"                 %% "os-lib"               % "0.8.0"
    val libFastparseLib =   "com.lihaoyi"                 %% "fastparse"            % "2.3.2"
    val libUpickle =        "com.lihaoyi"                 %% "upickle"              % "1.4.1"
    val libUjsonLib =       "com.lihaoyi"                 %% "ujson"                % "1.3.15" 
    val libCask =           "com.lihaoyi"                 %% "cask"                 % "0.7.11" //exclude("ch.qos.logback","logback-core")

    val libPrometheusClient =   "io.prometheus"           % "simpleclient"          % "0.10.0"
    
    val libCsv =            "com.github.tototoshi"        %% "scala-csv"            % "1.3.7"

    val libScodec =         "org.scodec"                  %% "scodec-core"          % "1.11.7"

    val libEnumeratum =     "com.beachape"                %% "enumeratum"          % "1.6.1"    
    val libDispatch =       "org.dispatchhttp"              %% "dispatch-core"              % dispatchVersion exclude("org.scala-lang.modules","scala-xml")
    val libJaxbApi =        "javax.xml.bind"                % "jaxb-api"                    % "2.3.0"
    val libScalaXml =       "org.scala-lang.modules"        %% "scala-xml"                  % "2.0.1" //"1.3.0"
    val libScalaParser =    "org.scala-lang.modules"        %% "scala-parser-combinators"   % "1.1.2"
    val libXs4s =           "com.scalawilliam"              %% "xs4s-core"                  % "0.9.1"

    val libKebsSpray =      "pl.iterators"                  %% "kebs-spray-json"            % "1.9.3"

    val libParq =             "com.github.mjakubowski84"      %% "parquet4s-core"                 % parq4sVersion
    val libParqAkka =         "com.github.mjakubowski84"      %% "parquet4s-akka"                 % parq4sVersion
    val libHadoop =           "org.apache.hadoop"             % "hadoop-client"                   % hadoopVersion
    val libHadoopLZO =        "hadoop-lzo"                    % "hadoop-lzo"                      % "0.4.15"
    val libSparkCore =        "org.apache.spark"              %% "spark-core"         % sparkVersion
    val libSparkSQL =         "org.apache.spark"              %% "spark-sql"          % sparkVersion

    val libSpark =          Seq(libSparkCore,libSparkSQL)

    val libSkelCore =       "io.syspulse"                 %% "skel-core"            % skelVersion
    val libSkelAuth =       "io.syspulse"                 %% "skel-auth-core"       % skelVersion
    val libSkelIngest =     "io.syspulse"                 %% "skel-ingest"          % skelVersion
    val libSkelIngestFlow = "io.syspulse"                 %% "ingest-flow"          % skelVersion
    val libSkelCrypto =     "io.syspulse"                 %% "skel-crypto"          % skelVersion
    val libSkelSerde =      "io.syspulse"                 %% "skel-serde"           % skelVersion
    
    // Projects
    val libCommon = Seq(libScalaLogging, libSlf4jApi, libLogback, libJanino, libTypesafeConfig )
    //val libPrometheus = Seq(libPrometheusClient,libPrometheusHttp,libPrometheusHotspot)
    val libHttp = Seq(libAkkaHttp,libAkkaHttpSpray,libAkkaHttpCors,libWsRsJakarta,libSwaggerAkkaHttp)
    val libTest = Seq(libScalaTest, libOsLib)

    val libPrometheus = Seq(libPrometheusClient)
    
    // val libSkel = Seq(libSkelCore,libSkelCrypto,libSkelAuth)
    val libSkel = Seq(libSkelCore,libSwaggerAkkaHttp)

    val libDB = Seq(libQuill,libQuillAsyncPostgres, libQuillAsyncMySQL, libMySQL, libPostgres)
    
    val libAkka = Seq(libAkkaActor,libAkkaActorTyped,libAkkaStream,libAkkaStreamTyped)

    val libXML = Seq(libDispatch,libJaxbApi,libScalaXml,libScalaParser,libXs4s)

    val libAeroware = Seq(libScopt,libUUID)
}
  
