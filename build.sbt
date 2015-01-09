name := "oerworldmap"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "com.github.jsonld-java" % "jsonld-java" % "0.5.1"
)
