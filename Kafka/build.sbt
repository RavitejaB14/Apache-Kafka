ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"

lazy val root = (project in file("."))
  .settings(
    name := "Kafka"
  )

libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.9.0"
