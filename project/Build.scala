import sbt.Keys._
import sbt._

object ProjectBuild extends Build {
  val scalaV = "2.11.5"

  val defaultScalacOptions = Seq()

  val defaultLibraryDependencies = Seq(
    
    )

  val defaultSettings = Defaults.defaultSettings ++ Seq(
    scalacOptions ++= defaultScalacOptions,
    libraryDependencies ++= defaultLibraryDependencies
  ) ++ Seq(scalaVersion := scalaV)

  lazy val cassandraTransactions = Project("cassandra-transactions", file(".")) aggregate(cassandraTransactionsAkka, cassandraTransactionsClient, cassandraTransactionsCore)

  lazy val cassandraTransactionsCore = Project("cassandra-transactions-core", 
    file("cassandra-transactions-core"), settings = defaultSettings) 

  lazy val cassandraTransactionsAkka = Project("cassandra-transactions-akka", 
    file("cassandra-transactions-akka"), settings = defaultSettings).dependsOn(cassandraTransactionsCore % "compile->compile")

  lazy val cassandraTransactionsClient = Project("cassandra-transactions-client", 
    file("cassandra-transactions-client"), settings = defaultSettings).dependsOn(cassandraTransactionsAkka % "compile->compile")
}