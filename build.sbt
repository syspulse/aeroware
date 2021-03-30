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


val sharedConfigDocker = Seq(
  maintainer := "Dev0 <dev0@syspulse.io>",
  dockerBaseImage := "openjdk:8-jre-alpine",
  dockerUpdateLatest := true,
  dockerUsername := Some("syspulse"),
  dockerExposedVolumes := Seq(s"${appDockerRoot}/logs",s"${appDockerRoot}/conf","/data"),
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

// assemblyMergeStrategy in assembly := {
  // case "application.conf" => MergeStrategy.concat
  // case "reference.conf" => MergeStrategy.concat
  // case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  // case PathList("META-INF/MANIFEST.MF", xs @ _*) => MergeStrategy.discard
  // case PathList("snakeyaml-1.27-android.jar", xs @ _*) => MergeStrategy.discard
  // case PathList("commons-logging-1.2.jar", xs @ _*) => MergeStrategy.discard
  // case x => MergeStrategy.first
// }


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
  .aggregate(core, gamet)
  .dependsOn(core, gamet)
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
      libraryDependencies ++= libCommon ++ libAeroware ++ libTest ++ Seq(),
    )

lazy val gamet = (project in file("aw-gamet"))
  .dependsOn(core)
  .settings (
    sharedConfig,
    sharedConfigAssembly,

    name := "aw-gamet",
    libraryDependencies ++= libCommon ++ libTest ++ Seq(
      libFastparseLib 
    ),
    
    // mainClass in run := Some(appBootClassGamet),
    // mainClass in assembly := Some(appBootClassGamet),
    // assemblyJarName in assembly := jarPrefix + appNameGamet + "-" + "assembly" + "-"+  appVersion + ".jar",
  )

