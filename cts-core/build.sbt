libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.6" % "provided" exclude("com.google.guava", "guava"),
  "com.google.guava" % "guava" % "16.0" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)