name := "AkkaDownloader"

organization := "nidkil.com"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"ch.qos.logback" % "logback-classic" % "1.1.2",
	"com.typesafe.akka" %% "akka-actor" % "2.3.6",
	"com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
	"org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
)