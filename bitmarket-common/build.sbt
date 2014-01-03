name := "Bitmarket Common"

// TODO: evaluate scalaxb as a Scalaish replacement of Axis2
libraryDependencies ++= Dependencies.axis2 ++ Seq(
  Dependencies.bitcoinj,
  Dependencies.netty,
  Dependencies.protobufRpc
)
