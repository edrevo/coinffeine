name := "Coinffeine GUI"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)

libraryDependencies ++= Dependencies.scalafx ++ Seq(
  Dependencies.zxing,
  "org.loadui" % "testFx" % "3.1.2"
)

unmanagedJars in Compile += Attributed.blank(file(scala.util.Properties.javaHome) / "/lib/jfxrt.jar")

fork := true

// testOptions in Test += Tests.Argument("-l", "UITest")

addCommandAlias("test", "test-only * -- -l UITest")

addCommandAlias("test-gui", "test-only * -- -n UITest")

jfxSettings

JFX.mainClass := Some("com.coinffeine.gui.Main")

JFX.title := "Coinffeine"

JFX.nativeBundles := "all"
