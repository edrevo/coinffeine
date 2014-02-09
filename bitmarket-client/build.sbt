name := "Bitmarket Client"

libraryDependencies ++= Dependencies.axis2 ++ Seq(
  Dependencies.netty,
  "com.google.protobuf" % "protobuf-java" % "2.4.1",
  "com.h2database" % "h2" % "1.3.175" % "test"
)
