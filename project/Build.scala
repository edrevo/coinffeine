import sbt._
import sbtaxis.Plugin.{SbtAxisKeys, sbtAxisSettings}
import sbtprotobuf.{ProtobufPlugin => PB}

object Build extends sbt.Build {

  object Versions {
    val axis2 = "1.6.2"
    val akka = "2.2.3"
  }

  object Dependencies {
    lazy val axis2 = Seq(
      "org.apache.axis2" % "axis2-kernel" % Versions.axis2,
      "org.apache.axis2" % "axis2-adb" % Versions.axis2,
      "org.apache.axis2" % "axis2-transport-http" % Versions.axis2,
      "org.apache.axis2" % "axis2-transport-local" % Versions.axis2,
      "org.apache.axis2" % "axis2-xmlbeans" % Versions.axis2
    )
    lazy val akka = Seq(
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka
    )
    lazy val bitcoinj = "com.google" % "bitcoinj" % "0.11"
    lazy val commonsConfig = "commons-configuration" % "commons-configuration" % "1.8"
    lazy val guava = "com.google.guava" % "guava" % "11.0.1"
    lazy val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
    lazy val jcommander = "com.beust" % "jcommander" % "1.32"
    lazy val jodaTime = "joda-time" % "joda-time" % "2.3"
    lazy val junit = "junit" % "junit" % "4.11" % "test"
    lazy val junitInterface = "com.novocode" % "junit-interface" % "0.9" % "test"
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
    settings = Defaults.defaultSettings ++ PB.protobufSettings ++ sbtAxisSettings ++ Seq(
      SbtAxisKeys.wsdlFiles += file("coinffeine-common/src/main/resources/wsdl/okpay.wsdl"),
      SbtAxisKeys.packageSpace := Some("com.coinffeine.common.paymentprocessor.okpay"),
      SbtAxisKeys.dataBindingName := Some("adb")
    ))
      dependsOn(commonTest % "test->compile")
    )

  lazy val commonTest = Project(
    id = "common-test",
    base = file("coinffeine-common-test"),
    settings = Defaults.defaultSettings ++ PB.protobufSettings
  )

  lazy val gui = (Project(id = "gui", base = file("coinffeine-gui"))
    dependsOn(client)
  )

  lazy val server = (Project(id = "server", base = file("coinffeine-server"))
    dependsOn(common % "compile->compile;test->test", commonTest % "test->compile")
  )
}
