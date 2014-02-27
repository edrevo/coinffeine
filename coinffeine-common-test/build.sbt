name := "Coinffeine Common Test"

libraryDependencies in ThisBuild ++= Dependencies.akka ++ Seq(
  Dependencies.protobufRpc,
  Dependencies.scalatest
)
