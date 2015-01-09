name := "oerworldmap"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

libraryDependencies ++= Seq(
  "com.github.jsonld-java" % "jsonld-java" % "0.5.1",
  "com.github.jknack" % "handlebars" % "2.0.0",
  "com.github.jknack" % "handlebars-jackson2" % "2.0.0"
)
