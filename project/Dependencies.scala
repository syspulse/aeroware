import sbt._

object Dependencies {

    // Versions
    lazy val versionScalaLogging = "3.9.2"
    lazy val akkaVersion    = "2.6.10"  
    lazy val akkaHttpVersion = "10.2.1"
    lazy val akkaKafkaVersion = "2.0.3"
    lazy val kafkaAvroSerVersion = "5.4.1"
    lazy val quillVersion = "3.6.0"
    
    lazy val skelVersion = "0.0.2"
    lazy val appVersion = "0.0.1"
    lazy val jarPrefix = "server-"
    
    lazy val appDockerRoot = "/app"

    val libSkelCore =       "io.syspulse"                 %% "skel-core"            % skelVersion

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

    val libCsv =            "com.github.tototoshi"        %% "scala-csv"            % "1.3.7"
    
    // Projects
    val libCommon = Seq(libScalaLogging, libLogback, libTypesafeConfig )
    val libTest = Seq(libScalaTest, libOsLib)

    val libSkel = Seq(libSkelCore)
    val libAeroware = Seq(libScopt,libUUID)

}
  