name := "Bitmarket Client"

libraryDependencies ++= Dependencies.axis2 ++ Seq(
  Dependencies.netty,
  "com.google.protobuf" % "protobuf-java" % "2.4.1"
)
