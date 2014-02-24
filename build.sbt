organization := "net.amoeba.play2"

name := "am-play-module"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.google.inject" % "guice" % "3.0" withSources ()
)     

play.Project.playScalaSettings
