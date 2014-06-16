name := "Coinffeine Client"

libraryDependencies ++= Seq(
  Dependencies.h2 % "test",
  Dependencies.jcommander,
  Dependencies.netty
)
