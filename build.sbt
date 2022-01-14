import scala.sys.process.Process
import Dependencies._
import com.typesafe.sbt.packager.docker._

Global / onChangedBuildSource := ReloadOnSourceChanges

Test / parallelExecution := true

// this will suppress scala-xml:1.2.0 dependency error in dispatchhttp
ThisBuild / evictionErrorLevel := Level.Info

initialize ~= { _ =>
  System.setProperty("config.file", "conf/application.conf")
}

fork := true
run / connectInput := true

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)
//enablePlugins(JavaAppPackaging, AshScriptPlugin)

// Huge Credits -> https://softwaremill.com/how-to-build-multi-platform-docker-image-with-sbt-and-docker-buildx
lazy val ensureDockerBuildx = taskKey[Unit]("Ensure that docker buildx configuration exists")
lazy val dockerBuildWithBuildx = taskKey[Unit]("Build docker images using buildx")
lazy val dockerBuildxSettings = Seq(
  ensureDockerBuildx := {
    if (Process("docker buildx inspect multi-arch-builder").! == 1) {
      Process("docker buildx create --use --name multi-arch-builder", baseDirectory.value).!
    }
  },
  dockerBuildWithBuildx := {
    streams.value.log("Building and pushing image with Buildx")
    dockerAliases.value.foreach(
      alias => Process("docker buildx build --platform=linux/arm64,linux/amd64 --push -t " +
        alias + " .", baseDirectory.value / "target" / "docker"/ "stage").!
    )
  },
  Docker / publish := Def.sequential(
    Docker / publishLocal,
    ensureDockerBuildx,
    dockerBuildWithBuildx
  ).value
)

val sharedConfigDocker = Seq(
  maintainer := "Dev0 <dev0@syspulse.io>",
  // openjdk:8-jre-alpine - NOT WORKING ON RP4+ (arm64). Crashes JVM in kubernetes
  dockerBaseImage := "openjdk:18-slim", //"openjdk:8u212-jre-alpine3.9", //"openjdk:8-jre-alpine",
  dockerUpdateLatest := true,
  dockerUsername := Some("syspulse"),
  dockerExposedVolumes := Seq(s"${appDockerRoot}/logs",s"${appDockerRoot}/conf",s"${appDockerRoot}/data","/data"),
  //dockerRepository := "docker.io",
  dockerExposedPorts := Seq(8080),

  Docker / defaultLinuxInstallLocation := appDockerRoot,

  Docker / daemonUserUid := None, //Some("1000"), 
  Docker / daemonUser := "daemon"
)

val sharedConfig = Seq(
    //retrieveManaged := true,  
    organization    := "io.syspulse",
    scalaVersion    := "2.13.6",
    name            := "aeroware",
    version         := appVersion,

    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:existentials", "-language:implicitConversions", "-language:higherKinds", "-language:reflectiveCalls", "-language:postfixOps"),
    javacOptions ++= Seq("-target", "1.8", "-source", "1.8"),
//    manifestSetting,
//    publishSetting,
    resolvers ++= Seq(Opts.resolver.sonatypeSnapshots, Opts.resolver.sonatypeReleases),
    crossVersion := CrossVersion.binary,
    resolvers ++= Seq(
      "spray repo"         at "https://repo.spray.io/",
      "sonatype releases"  at "https://oss.sonatype.org/content/repositories/releases/",
      "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "typesafe repo"      at "https://repo.typesafe.com/typesafe/releases/",
      "confluent repo"     at "https://packages.confluent.io/maven/",
      "consensys repo"     at "https://artifacts.consensys.net/public/maven/maven/",
      "consensys teku"     at "https://artifacts.consensys.net/public/teku/maven/"
    ),
  )


val sharedConfigAssembly = Seq(
  assembly / assemblyMergeStrategy := {
      case x if x.contains("module-info.class") => MergeStrategy.discard
      case x if x.contains("io.netty.versions.properties") => MergeStrategy.first
      //case x if x.contains("StaticMarkerBinder.class") => MergeStrategy.first // logback-classic-1.2.8.jar vs. log4j-slf4j-impl-2.13.3.jar
      case x => {
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
      }
  },
  assembly / assemblyExcludedJars := {
    val cp = (assembly / fullClasspath).value
    cp filter { f =>
      f.data.getName.contains("snakeyaml-1.27-android.jar")  || 
      f.data.getName.contains("log4j-slf4j-impl-2.13.3.jar") 
      // ||
      // f.data.getName.container("spark-core_2.11-2.0.1.jar")
    }
  },
  
  
  assembly / test := {}
)

lazy val root = (project in file("."))
  .aggregate(core, gamet, adsb_core, adsb_ingest, adsb_tools, adsb_live, gpx_core, adsb_miner)
  .dependsOn(core, gamet, adsb_core, adsb_ingest, adsb_tools, adsb_live, gpx_core, adsb_miner)
  .disablePlugins(sbtassembly.AssemblyPlugin) // this is needed to prevent generating useless assembly and merge error
  .settings(
    
    sharedConfig,
    sharedConfigDocker
  )

lazy val core = (project in file("aw-core"))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "aw-core",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(),
)

lazy val data = (project in file("aw-data"))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "aw-data",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(),
)

lazy val gamet = (project in file("aw-gamet"))
  .dependsOn(core)
  .settings (
    sharedConfig,
    sharedConfigAssembly,

    name := "aw-gamet",
    libraryDependencies ++= libCommon ++ libTest ++ libSkel ++ Seq(
      libEnumeratum,
      libFastparseLib 
    )
)

lazy val adsb_core = (project in file("aw-adsb/adsb-core"))
  .dependsOn(core,data)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "adsb-core",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(
        libScodec,
        libEnumeratum
      ),
)

lazy val adsb_ingest = (project in file("aw-adsb/adsb-ingest"))
  .dependsOn(core,data,adsb_core)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings (

    sharedConfig,
    sharedConfigAssembly,
    sharedConfigDocker,
    dockerBuildxSettings,

    name := appNameAdsbIngest,
    run / mainClass := Some(appBootClassAdsbIngest),
    assembly / mainClass := Some(appBootClassAdsbIngest),
    Compile / mainClass := Some(appBootClassAdsbIngest), // <-- This is very important for DockerPlugin generated stage1 script!
    assembly / assemblyJarName := jarPrefix + appNameAdsbIngest + "-" + "assembly" + "-"+  appVersion + ".jar",

    Universal / mappings += file(baseDirectory.value.getAbsolutePath+"/conf/application.conf") -> "conf/application.conf",
    Universal / mappings += file(baseDirectory.value.getAbsolutePath+"/conf/logback.xml") -> "conf/logback.xml",
    bashScriptExtraDefines += s"""addJava "-Dconfig.file=${appDockerRoot}/conf/application.conf"""",
    bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=${appDockerRoot}/conf/logback.xml"""",

    libraryDependencies ++= libAkka ++ libSkel ++ libPrometheus ++ Seq(
      libAlpakkaFile,
      libUjsonLib,
      libUpickle
    ),
  )

lazy val adsb_miner = (project in file("aw-adsb/adsb-miner"))
  .dependsOn(core,data,adsb_core,adsb_ingest)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings (

    sharedConfig,
    sharedConfigAssembly,
    sharedConfigDocker,
    dockerBuildxSettings,

    name := appNameAdsbMiner,
    run / mainClass := Some(appBootClassAdsbMiner),
    assembly / mainClass := Some(appBootClassAdsbMiner),
    Compile / mainClass := Some(appBootClassAdsbMiner), // <-- This is very important for DockerPlugin generated stage1 script!
    assembly / assemblyJarName := jarPrefix + appNameAdsbMiner + "-" + "assembly" + "-"+  appVersion + ".jar",

    Universal / mappings += file(baseDirectory.value.getAbsolutePath+"/conf/application.conf") -> "conf/application.conf",
    Universal / mappings += file(baseDirectory.value.getAbsolutePath+"/conf/logback.xml") -> "conf/logback.xml",
    bashScriptExtraDefines += s"""addJava "-Dconfig.file=${appDockerRoot}/conf/application.conf"""",
    bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=${appDockerRoot}/conf/logback.xml"""",

    libraryDependencies ++= libAkka ++ libSkel ++ libPrometheus ++ Seq(
      libAlpakkaFile,
      libUjsonLib,
      libUpickle
    ),
  )



lazy val adsb_tools = (project in file("aw-adsb/adsb-tools"))
  .dependsOn(core, data,adsb_core)
  .enablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      sharedConfigAssembly,
      name := "adsb-tools",
      libraryDependencies ++= libCommon ++ libAeroware ++ libSkel ++ Seq(
        libCask  
      ),
  )

lazy val adsb_live = (project in file("aw-adsb/adsb-live"))
  .dependsOn(core, data, adsb_core)
  .enablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      sharedConfigAssembly,
      name := "adsb-live",
      libraryDependencies ++= libCommon ++ libAeroware ++ libSkel ++ libTest ++ Seq(
        libAkkaTestkitType % Test  
      ),
  )

// use scalaxb sbt target to generate XML bindings
lazy val gpx_core = (project in file("aw-gpx/gpx-core"))
  .dependsOn(core)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .enablePlugins(ScalaxbPlugin)
  .settings (
      sharedConfig,
      name := "gpx-core",

      scalaxbPackageName in (Compile, scalaxb) := "generated",
      // scalaxbAutoPackages in (Compile, scalaxb) := true,
      scalaxbDispatchVersion in (Compile, scalaxb) := dispatchVersion,

      libraryDependencies ++= libCommon ++ libTest ++ libXML ++ Seq(
        
      ),
  )
