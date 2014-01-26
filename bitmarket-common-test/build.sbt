name := "Bitmarket Common Test"

libraryDependencies in ThisBuild ++= Dependencies.axis2 ++ Dependencies.akka ++ Seq(
  Dependencies.protobufRpc,
  Dependencies.scalatest
)
