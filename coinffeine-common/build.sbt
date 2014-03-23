name := "Coinffeine Common"

libraryDependencies ++= Dependencies.akka ++ Seq(
  Dependencies.bitcoinj,
  Dependencies.dispatch,
  Dependencies.jodaConvert,
  Dependencies.netty,
  Dependencies.protobufRpc,
  Dependencies.reflections % "test"
)
