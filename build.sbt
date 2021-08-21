import scala.sys.process.Process
import Dependencies._
import com.typesafe.sbt.packager.docker._

parallelExecution in Test := true

initialize ~= { _ =>
  System.setProperty("config.file", "conf/application.conf")
}

fork := true
connectInput in run := true

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
  publish in Docker := Def.sequential(
    publishLocal in Docker,
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

  defaultLinuxInstallLocation in Docker := appDockerRoot,

  daemonUserUid in Docker := None, //Some("1000"), 
  daemonUser in Docker := "daemon"
)

val sharedConfig = Seq(
    //retrieveManaged := true,  
    organization    := "io.syspulse",
    scalaVersion    := "2.13.3",
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
    ),
  )


val sharedConfigAssembly = Seq(
  assemblyMergeStrategy in assembly := {
      case x if x.contains("module-info.class") => MergeStrategy.discard
      case x => {
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
      }
  },
  assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter { f =>
      f.data.getName.contains("snakeyaml-1.27-android.jar") 
      //||
      //f.data.getName == "spark-core_2.11-2.0.1.jar"
    }
  },
  
  test in assembly := {}
)

lazy val root = (project in file("."))
  .aggregate(core, gamet, adsb_core,adsb_ingest)
  .dependsOn(core, gamet, adsb_core, adsb_ingest)
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

lazy val gamet = (project in file("aw-gamet"))
  .dependsOn(core)
  .settings (
    sharedConfig,
    sharedConfigAssembly,

    name := "aw-gamet",
    libraryDependencies ++= libCommon ++ libTest ++ libSkel ++ Seq(
      libFastparseLib 
    ),
    
    // mainClass in run := Some(appBootClassGamet),
    // mainClass in assembly := Some(appBootClassGamet),
    // assemblyJarName in assembly := jarPrefix + appNameGamet + "-" + "assembly" + "-"+  appVersion + ".jar",
)

lazy val adsb_core = (project in file("aw-adsb/adsb-core"))
  .dependsOn(core)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings (
      sharedConfig,
      name := "adsb-core",
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ libSkel ++ Seq(
        libScodec
      ),
)

lazy val adsb_ingest = (project in file("aw-adsb/adsb-ingest"))
  .dependsOn(core,adsb_core)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings (

    sharedConfig,
    sharedConfigAssembly,
    sharedConfigDocker,
    dockerBuildxSettings,

    mappings in Universal += file("conf/application.conf") -> "conf/application.conf",
    mappings in Universal += file("conf/logback.xml") -> "conf/logback.xml",
    bashScriptExtraDefines += s"""addJava "-Dconfig.file=${appDockerRoot}/conf/application.conf"""",
    bashScriptExtraDefines += s"""addJava "-Dlogback.configurationFile=${appDockerRoot}/conf/logback.xml"""",

    name := appNameAdsb,
    libraryDependencies ++= libAkka ++ libSkel ++ libPrometheus ++ Seq(
      libAlpakkaFile,
      libUjsonLib,
      libUpickle
    ),
    
    mainClass in run := Some(appBootClassAdsb),
    mainClass in assembly := Some(appBootClassAdsb),
    assemblyJarName in assembly := jarPrefix + appNameAdsb + "-" + "assembly" + "-"+  appVersion + ".jar",

)

