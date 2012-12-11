import sbt._
import sbt.Keys._

object SparkStreamingBuild extends Build {

  lazy val sparkstreamingpluggablereceivers = Project(
    id = "spark-streaming-pluggable-receivers",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      scalacOptions := Seq("-deprecation", "-unchecked", "-optimize"),
      name := "spark-streaming-pluggable-receivers",
      organization := "com.imaginea",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      retrieveManaged := true,
      resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
      libraryDependencies ++= Seq("com.typesafe.akka" % "akka-zeromq" % "2.0.3",
                                  "org.spark-project" %% "spark-streaming" % "0.7.0-SNAPSHOT")
    )
  )
}
