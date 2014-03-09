import sbt._
import sbt.Keys._
import sbtprotobuf.{ProtobufPlugin => PB}
import sbtscalaxb.Plugin.scalaxbSettings
import sbtscalaxb.Plugin.ScalaxbKeys._

object Build extends sbt.Build {

  object Versions {
    val akka = "2.2.3"
  }

  object Dependencies {
    lazy val akka = Seq(
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka
    )
    lazy val bitcoinj = "com.google" % "bitcoinj" % "0.11"
    lazy val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.10.1"
    lazy val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
    lazy val jcommander = "com.beust" % "jcommander" % "1.32"
    lazy val jodaTime = "joda-time" % "joda-time" % "2.3"
    lazy val jodaConvert = "org.joda" % "joda-convert" % "1.2"
    lazy val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.0.13"
    lazy val logbackCore = "ch.qos.logback" % "logback-core" % "1.0.13"
    lazy val mockito = "org.mockito" % "mockito-all" % "1.9.5" % "test"
    lazy val netty = "io.netty" % "netty-all" % "4.0.12.Final"
    lazy val protobuf = "com.google.protobuf" % "protobuf-java" % "2.5.0"
    lazy val protobufRpc = "com.googlecode.protobuf-rpc-pro" % "protobuf-rpc-pro-duplex" % "3.0.8"
    lazy val scalatest = "org.scalatest" %% "scalatest" % "1.9.1"
    lazy val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"
  }

  lazy val root = (Project(id = "coinffeine", base = file("."))
    aggregate(client, common, commonTest, gui, server)
  )

  lazy val client = (Project(id = "client", base = file("coinffeine-client"))
    dependsOn(common % "compile->compile;test->test", commonTest % "test->compile")
  )

  lazy val common = (Project(
    id = "common",
    base = file("coinffeine-common"),
      settings = Defaults.defaultSettings ++ PB.protobufSettings ++ scalaxbSettings ++ Seq(
      sourceGenerators in Compile <+= scalaxb in Compile,
      packageName in scalaxb in Compile := "com.coinffeine.common.paymentprocessor.okpay"
    ))
      dependsOn(commonTest % "test->compile")
    )

  lazy val commonTest = Project(
    id = "common-test",
    base = file("coinffeine-common-test"),
    settings = Defaults.defaultSettings ++ PB.protobufSettings
  )

  lazy val gui = (Project(id = "gui", base = file("coinffeine-gui"))
    dependsOn client
  )

  lazy val server = (Project(id = "server", base = file("coinffeine-server"))
    dependsOn(common % "compile->compile;test->test", commonTest % "test->compile")
  )

  lazy val test = (Project(id = "test", base = file("coinffeine-test"))
    dependsOn(client, server, common, commonTest % "compile->compile;test->compile")
  )
}
