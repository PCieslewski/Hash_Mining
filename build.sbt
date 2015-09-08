name := "PiCalc"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.12",
  "com.typesafe.akka" %% "akka-agent" % "2.3.12",
  "com.typesafe.akka" %% "akka-camel" % "2.3.12",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.12",
  "com.typesafe.akka" %% "akka-remote" % "2.3.12",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.12"
)