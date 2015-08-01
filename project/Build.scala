import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._

object ProjectBuild extends Build {
  val scalaV = "2.11.6"

  lazy val assemblyToC = TaskKey[Unit]("assembly-to-c", "Assembly fat jar and copy it over to Cassandra's lib directory")

  val defaultScalacOptions = Seq(

  )

  val defaultLibraryDependencies = Seq(

    )

  val taskDefs = Seq(
    assemblyToC <<= (assembly, baseDirectory, name, streams) map { (f: File, baseDir: File, n: String, s: TaskStreams) =>
      val log = s.log
      val cassandraLibDirectory = baseDir / ".." / ".." / "cassandra" / "lib"
      IO.delete(IO.listFiles(cassandraLibDirectory, n + "*"))
      val targetFile: File = cassandraLibDirectory / f.getName
      IO.copyFile(f, targetFile)

      log.info("Assembly to Cassandra copied jar to: " + targetFile.getPath)
    }
  )

  val defaultSettings = Defaults.defaultSettings ++ Seq(
    scalacOptions ++= defaultScalacOptions,
    libraryDependencies ++= defaultLibraryDependencies
  ) ++ Seq(scalaVersion := scalaV) ++ taskDefs

  lazy val cassandraTransactions = Project("cassandra-transactions", file(".")) aggregate(cassandraTransactionsAkka, cassandraTransactionsClient, cassandraTransactionsCore)

  lazy val cassandraTransactionsCore = Project("cassandra-transactions-core",
    file("cassandra-transactions-core"), settings = defaultSettings)

  lazy val cassandraTransactionsAkka = Project("cassandra-transactions-akka",
    file("cassandra-transactions-akka"),
    settings = defaultSettings
    ).dependsOn(cassandraTransactionsCore % "compile->compile")

  lazy val cassandraTransactionsClient = Project("cassandra-transactions-client",
    file("cassandra-transactions-client"), settings = defaultSettings).dependsOn(cassandraTransactionsAkka % "compile->compile")

  lazy val ctsExampleMusic = Project("cts-example-music",
                                     file("cts-example-music"),
                                     settings = defaultSettings).
    dependsOn(cassandraTransactionsClient % "compile->compile")


}