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
  ) ++ Seq(scalaVersion := scalaV)

  lazy val cts = Project("cts", file(".")) aggregate(ctsServerExt, ctsClientExt, ctsCore)

  lazy val ctsCore = Project("cts-core",
    file("cts-core"), settings = defaultSettings)


  lazy val ctsServerExt = Project("cts-server-ext",
    file("cts-server-ext"),
    settings = defaultSettings ++ taskDefs
    ).dependsOn(ctsCore % "compile->compile")

  lazy val ctsClientExt = Project("cts-client-ext",
    file("cts-client-ext"), settings = defaultSettings).dependsOn(ctsServerExt % "compile->compile")

  lazy val ctsExampleMusic = Project("cts-example-music",
                                     file("cts-example-music"),
                                     settings = defaultSettings).
    dependsOn(ctsClientExt % "compile->compile")


}