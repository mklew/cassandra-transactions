import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.MergeStrategy

object ProjectBuild extends Build {
  val scalaV = "2.11.6"

  lazy val assemblyToC = TaskKey[Unit]("assembly-to-c", "Assembly fat jar and copy it over to Cassandra's lib directory")
  lazy val toC = TaskKey[Unit]("to-c", "Assembly fat jar and copy it over to Cassandra's lib directory")

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
    },

    toC := {
            assemblyToC.value
    },

    assemblyMergeStrategy in assembly := {
      case "META-INF/io.netty.versions.properties"                 => MergeStrategy.first
      //  case PathList(ps @ _*) if ps.last =>
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

  val defaultSettings = Defaults.defaultSettings ++ Seq(
    scalacOptions ++= defaultScalacOptions,
    libraryDependencies ++= defaultLibraryDependencies
  ) ++ Seq(scalaVersion := scalaV)

  lazy val cts = Project("cts", file(".")) aggregate(cassandra, ctsServerExt, ctsClientExt, ctsCore, ctsAkkaDeps, ctsGraph)

  lazy val cassandra = Project("cassandra-link", file("cassandra-link"), settings = defaultSettings ++ Seq(
    javaSource in Compile := baseDirectory.value / "src" / "java",
    autoScalaLibrary := false,
    resourceDirectory in Compile := baseDirectory.value / "src" / "resources",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "gen-java",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "interface" / "thrift" / "gen-java",
    unmanagedJars in Compile ++= {
      val base = baseDirectory.value
      val baseDirectories = (base / "lib") +++ (base / "build" / "lib" / "jars")
      val customJars = (baseDirectories ** "*.jar")
      customJars.classpath
    }
  ))

  lazy val ctsAkkaDeps = Project("cts-akka-deps", file("cts-akka-deps"), settings = defaultSettings ++ taskDefs)

  lazy val ctsCore = Project("cts-core",
    file("cts-core"), settings = defaultSettings)

  lazy val ctsGraph = Project("cts-graph",
    file("cts-graph"), settings = defaultSettings)

  lazy val ctsServerExt = Project("cts-server-ext",
    file("cts-server-ext"),
    settings = defaultSettings ++ taskDefs
    ).dependsOn(ctsCore % "compile->compile").dependsOn(cassandra % "compile->compile")

  lazy val ctsClientExt = Project("cts-client-ext",
    file("cts-client-ext"), settings = defaultSettings).dependsOn(ctsCore % "compile->compile")

  lazy val ctsExampleMusic = Project("cts-example-music",
                                     file("cts-example-music"),
                                     settings = defaultSettings).
    dependsOn(ctsClientExt % "compile->compile")


}