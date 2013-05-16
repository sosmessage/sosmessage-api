import com.typesafe.startscript.StartScriptPlugin

organization := "fr.arnk"

name := "sosmessage-api"

version := "2.5-SNAPSHOT"

scalaVersion := "2.10.0"

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.1.0",
  "net.databinder" %% "unfiltered-filter" % "0.6.7",
  "net.databinder" %% "unfiltered-netty-server" % "0.6.7",
  "net.databinder" %% "unfiltered-json" % "0.6.7",
  "org.mongodb" %% "casbah" % "2.5.0",
  "org.streum" %% "configrity-core" % "1.0.0",
  "javax.mail" % "mail" % "1.4.4",
  "ch.qos.logback" % "logback-classic" % "0.9.28",
  "net.databinder" %% "unfiltered-spec" % "0.6.7" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

scalariformSettings

parallelExecution in Test := false
