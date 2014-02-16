import AssemblyKeys._

name := "Coinffeine Server"

assemblySettings

jarName in assembly := "coinffeine-server-standalone.jar"

mainClass in assembly := Some("com.coinffeine.Main")

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter { _.data.getName.contains("jaxen") } // Exclude duplicated xmlbeans definitions
}

libraryDependencies ++= Dependencies.akka ++ Seq(
  Dependencies.jcommander
)
