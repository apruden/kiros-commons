name := """kiros-commons"""

version := "1.0"

scalaVersion := "2.11.6"

//uncomment the following line if you want cross build
// crossScalaVersions := Seq("2.10.4", "2.11.6")

scalacOptions ++=  Seq(
  "-deprecation",
  "-unchecked",
  "-feature"
)

libraryDependencies ++= {
  val akkaV = "2.4.4"
  Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.3.11",
	"com.typesafe.akka" %% "akka-http-experimental" % akkaV,
	"com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
	"com.typesafe.akka" %% "akka-stream" % akkaV,
	"com.typesafe.akka" %% "akka-stream-testkit" % akkaV,
	"org.scalatest" %% "scalatest" % "2.2.1" % "test",
  	"ch.qos.logback" % "logback-classic" % "1.1.2")
}

resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
    "RoundEights" at "http://maven.spikemark.net/roundeights"
)

scalariformSettings

//uncomment the following line if you want a java app packaging
// enablePlugins(JavaAppPackaging)
// enablePlugins(UniversalPlugin)
