name := "Coinffeine Common"

ScoverageSbtPlugin.ScoverageKeys.excludedPackages in ScoverageSbtPlugin.scoverage :=
  "scalaxb;soapenvelope11;.*generated.*;.*protobuf.*"

libraryDependencies ++= Dependencies.akka ++ Seq(
  Dependencies.bitcoinj,
  Dependencies.dispatch,
  Dependencies.jodaConvert,
  Dependencies.netty,
  Dependencies.protobufRpc,
  Dependencies.reflections % "test"
)
