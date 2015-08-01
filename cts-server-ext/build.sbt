val akkaV = "2.3.12"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaV % "provided",
  "com.typesafe.akka" %% "akka-cluster" % akkaV % "provided", // TODO This has to be removed.
  "com.typesafe.akka" %% "akka-slf4j" % akkaV % "provided",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.6" % "provided"
)

Keys.fork in run := true

mainClass in (Compile, run) := Some("mklew.cts.Boot")

net.virtualvoid.sbt.graph.Plugin.graphSettings

