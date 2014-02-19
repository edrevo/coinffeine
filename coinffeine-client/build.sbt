name := "Coinffeine Client"

libraryDependencies ++= Dependencies.axis2 ++ Seq(
  Dependencies.netty,
  "com.h2database" % "h2" % "1.3.175" % "test"
)
