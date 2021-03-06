name := "vectorclock"
version := "1.0"
 
scalaVersion := "2.11.7"

EclipseKeys.withSource := true
 
resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.14",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14"
)
