organization := "fr.arnk"

name := "sosmessage-api"

version := "2.1-SNAPSHOT"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

resolvers += Classpaths.typesafeResolver

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor" % "2.0",
  "net.databinder" %% "unfiltered-filter" % "0.6.1",
  "net.databinder" %% "unfiltered-netty-server" % "0.6.1",
  "net.databinder" %% "unfiltered-json" % "0.6.1",
  "org.mongodb" %% "casbah" % "2.4.1",
  "org.streum" %% "configrity-core" % "0.10.2",
  "javax.mail" % "mail" % "1.4.4",
  "ch.qos.logback" % "logback-classic" % "0.9.28",
  "net.databinder" %% "unfiltered-spec" % "0.6.1" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

scalariformSettings

parallelExecution in Test := false
