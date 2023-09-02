import scala.sys.process.Process
import Dependencies._
import com.typesafe.sbt.packager.docker.DockerAlias
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

val dockerRegistryLocal = Seq(
  dockerRepository := Some("docker.u132.net:5000"),
  dockerUsername := Some("syspulse"),
  // this fixes stupid idea of adding registry in publishLocal 
  dockerAlias := DockerAlias(registryHost=None,username = dockerUsername.value, name = name.value, tag = Some(version.value))
)

val dockerRegistryDockerHub = Seq(
  dockerUsername := Some("syspulse")
)

val sharedConfigDocker = Seq(
  maintainer := "Dev0 <dev0@syspulse.io>",
  // openjdk:8-jre-alpine - NOT WORKING ON RP4+ (arm64). Crashes JVM in kubernetes
  // dockerBaseImage := "openjdk:8u212-jre-alpine3.9", //"openjdk:8-jre-alpine",

  //dockerBaseImage := "openjdk:8-jre-alpine",
  dockerBaseImage := "openjdk:18-slim",
  
  dockerUpdateLatest := true,
  dockerUsername := Some("syspulse"),
  dockerExposedVolumes := Seq(s"${appDockerRoot}/logs",s"${appDockerRoot}/conf",s"${appDockerRoot}/data","/data"),
  //dockerRepository := "docker.io",
  dockerExposedPorts := Seq(8080),

  Docker / defaultLinuxInstallLocation := appDockerRoot,

  Docker / daemonUserUid := None, //Some("1000"), 
  Docker / daemonUser := "daemon"
) ++ dockerRegistryLocal


val sharedConfig = Seq(
    //retrieveManaged := true,  
    organization    := "io.syspulse",
    scalaVersion    := "2.13.9",
    name            := "aeroware",
    version         := awVersion,

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
      case x if x.contains("slf4j/impl/StaticMarkerBinder.class") => MergeStrategy.last
      case x if x.contains("slf4j/impl/StaticMDCBinder.class") => MergeStrategy.last
      case x if x.contains("slf4j/impl/StaticLoggerBinder.class") => MergeStrategy.last
      case x if x.contains("google/protobuf") => MergeStrategy.first
      case x => {
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
      }
  },
  assembly / assemblyExcludedJars := {
    val cp = (assembly / fullClasspath).value
    cp filter { f =>
      f.data.getName.contains("snakeyaml-1.27-android.jar") || 
      f.data.getName.contains("jakarta.activation-api-1.2.1") || 
      f.data.getName.contains("log4j-1.2.15.jar") ||
      f.data.getName.contains("log4j-1.2.17.jar") ||
      f.data.getName.contains("slf4j-api-1") ||
      f.data.getName.contains("logback-classic-1.2")
    }
  },
  
  assembly / test := {}
)

def appDockerConfig(appName:String,appMainClass:String) = 
  Seq(
    name := appName,

    run / mainClass := Some(appMainClass),
    assembly / mainClass := Some(appMainClass),
    Compile / mainClass := Some(appMainClass), // <-- This is very important for DockerPlugin generated stage1 script!
    assembly / assemblyJarName := jarPrefix + appName + "-" + "assembly" + "-"+  awVersion + ".jar",

    Universal / mappings += file(baseDirectory.value.getAbsolutePath+"/conf/application.conf") -> "conf/application.conf",
    Universal / mappings += file(baseDirectory.value.getAbsolutePath+"/conf/logback.xml") -> "conf/logback.xml",
    bashScriptExtraDefines += s"""addJava "-Dconfig.file=${appDockerRoot}/conf/application.conf"""",
    bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=${appDockerRoot}/conf/logback.xml"""",   
  )

def appAssemblyConfig(appName:String,appMainClass:String) = 
  Seq(
    name := appName,
    run / mainClass := Some(appMainClass),
    assembly / mainClass := Some(appMainClass),
    Compile / mainClass := Some(appMainClass),
    assembly / assemblyJarName := jarPrefix + appName + "-" + "assembly" + "-"+  awVersion + ".jar",
  )

// ======================================================================================================================
lazy val root = (project in file("."))
  .aggregate(
    core, 
    gamet, 
    metar,
    notam,
    sigmet,
    adsb_core, 
    adsb_ingest, 
    adsb_tools, 
    adsb_live, 
    gpx_core, 
    adsb_mesh, 
    adsb_miner, 
    adsb_validator,    
  )
  .dependsOn(
    core, 
    gamet,
    metar, 
    notam,
    sigmet,
    adsb_core, 
    adsb_ingest, 
    adsb_tools, 
    adsb_live, 
    gpx_core, 
    adsb_mesh,
    adsb_miner, 
    adsb_validator
  )
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

lazy val aircraft = (project in file("aw-aircraft"))
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "aw-aircraft",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(),
)

lazy val adsb_core = (project in file("aw-adsb/adsb-core"))
  .dependsOn(core,aircraft)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "adsb-core",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(
        libScodec,
        libEnumeratum
      ),
)

lazy val adsb_mesh = (project in file("aw-adsb/adsb-mesh"))
  .dependsOn(core,adsb_core,adsb_ingest)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "adsb-mesh",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(
        
        libAlpakkaFile,
        libUjsonLib,
        libUpickle,
        libAlpakkaMQTT,
        libAlpakkaPaho
      ),
)

lazy val adsb_ingest = (project in file("aw-adsb/adsb-ingest"))
  .dependsOn(core,aircraft,adsb_core)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings (    
    sharedConfig,
    sharedConfigAssembly,
    sharedConfigDocker,
    dockerBuildxSettings,

    appDockerConfig(appNameAdsbIngest,appBootClassAdsbIngest),

    libraryDependencies ++= libAkka ++ libSkel ++ libPrometheus ++ Seq(
      libSkelIngest,
      libSkelIngestFlow,

      libKebsSpray, // enumerator json
      
      libAlpakkaFile,
      libUjsonLib,
      libUpickle,

      libSkelSerde
    ),
  )

lazy val adsb_miner = (project in file("aw-adsb/adsb-miner"))
  .dependsOn(core,aircraft,adsb_core,adsb_ingest,adsb_mesh)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings (

    sharedConfig,
    sharedConfigAssembly,
    sharedConfigDocker,
    dockerBuildxSettings,

    appDockerConfig(appNameAdsbMiner,appBootClassAdsbMiner),

    libraryDependencies ++= libAkka ++ libSkel ++ libPrometheus ++ Seq(
      libSkelIngest,
      libSkelIngestFlow,
      
      libAlpakkaFile,
      libUjsonLib,
      libUpickle,
      //libAlpakkaMQTT,
      libAlpakkaPaho
    ),
    assembly / packageOptions += sbt.Package.ManifestAttributes("Multi-Release" -> "true")    
  )

lazy val adsb_validator = (project in file("aw-adsb/adsb-validator"))
  .dependsOn(core,aircraft,adsb_core,adsb_ingest,adsb_mesh)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings (

    sharedConfig,
    sharedConfigAssembly,
    sharedConfigDocker,
    dockerBuildxSettings,

    appDockerConfig(appNameAdsbValidator,appBootClassAdsbValidator),

    libraryDependencies ++= libAkka ++ libSkel ++ libPrometheus ++ Seq(
      libSkelIngest,
      libSkelIngestFlow,

      libAlpakkaFile,
      libUjsonLib,
      libUpickle,
      //libAlpakkaMQTT,
      libAlpakkaPaho
    ),
    assembly / packageOptions += sbt.Package.ManifestAttributes("Multi-Release" -> "true")
  )


lazy val adsb_tools = (project in file("aw-adsb/adsb-tools"))
  .dependsOn(core, aircraft,adsb_core)
  .enablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      sharedConfigAssembly,

      appAssemblyConfig("adsb-live","io.syspulse.aeroware.adsb.tools.AppPlayer"),

      libraryDependencies ++= libCommon ++ libAeroware ++ libSkel ++ Seq(
        libCask  
      ),
  )

lazy val adsb_live = (project in file("aw-adsb/adsb-live"))
  .dependsOn(core, aircraft, adsb_core)
  .enablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      sharedConfigAssembly,
      
      appAssemblyConfig("adsb-live","io.syspulse.aeroware.adsb.live.App"),

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

lazy val metar = (project in file("aw-metar"))
  .dependsOn(core)
  .settings (
    sharedConfig,
    sharedConfigAssembly,

    name := "aw-metar",
    libraryDependencies ++= libCommon ++ libTest ++ libSkel ++ Seq(
      libEnumeratum,
      libFastparseLib 
    )
  )

lazy val notam = (project in file("aw-notam"))
  .dependsOn(core)
  .settings (
    sharedConfig,
    sharedConfigAssembly,

    name := "aw-notam",
    libraryDependencies ++= libCommon ++ libTest ++ libSkel ++ Seq(
      libEnumeratum,
      libFastparseLib 
    )
)

lazy val sigmet = (project in file("aw-sigmet"))
  .dependsOn(core)
  .settings (
    sharedConfig,
    sharedConfigAssembly,

    name := "aw-sigmet",
    libraryDependencies ++= libCommon ++ libTest ++ libSkel ++ Seq(
      libEnumeratum,
      libFastparseLib 
    )
  )