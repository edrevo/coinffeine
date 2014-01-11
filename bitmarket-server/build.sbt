import AssemblyKeys._

name := "Bitmarket Server"

assemblySettings

jarName in assembly := "bitmarket-server-standalone.jar"

mainClass in assembly := Some("com.bitwise.bitmarket.registry.Main")

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter { _.data.getName.contains("jaxen") } // Exclude duplicated xmlbeans definitions
}

libraryDependencies ++= Dependencies.akka ++ Seq(
  Dependencies.jcommander
)
