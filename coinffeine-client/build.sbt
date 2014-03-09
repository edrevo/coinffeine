name := "Coinffeine Client"

libraryDependencies ++= Seq(
  Dependencies.jcommander,
  Dependencies.netty,
  "com.h2database" % "h2" % "1.3.175" % "test"
)
