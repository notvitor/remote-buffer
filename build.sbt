import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

val project = Project(
  id = "remote-buffer",
  base = file("."),
  settings = Defaults.coreDefaultSettings ++ SbtMultiJvm.multiJvmSettings ++ Seq(
    name := "remote-buffer",
    scalaVersion := "2.11.7",
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"),
    libraryDependencies ++= Seq(
      "com.typesafe"          %  "config"                  % "1.3.0",
      "com.typesafe.akka"     %% "akka-actor"              % "2.4.1",
      "com.typesafe.akka"     %% "akka-remote"             % "2.4.1",
      "com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.0"),

    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    Keys.fork in run := true,
    mainClass in (Compile, run) := Some("Main")
  )
) configs (MultiJvm)
