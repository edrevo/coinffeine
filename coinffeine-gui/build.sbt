name := "Coinffeine GUI"

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)

libraryDependencies ++= Dependencies.scalafx

unmanagedJars in Compile += Attributed.blank(file(scala.util.Properties.javaHome) / "/lib/jfxrt.jar")

fork := true

jfxSettings

JFX.mainClass := Some("com.coinffeine.gui.Main")

JFX.title := "Coinffeine"

JFX.nativeBundles := "all"
