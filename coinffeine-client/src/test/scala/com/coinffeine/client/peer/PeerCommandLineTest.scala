package com.coinffeine.client.peer

import com.beust.jcommander.ParameterException

import com.coinffeine.common.{PeerConnection, UnitTest}

class PeerCommandLineTest extends UnitTest {

  "The command line parser" must "parse local port and broker address" in {
    val cli = parseCli("--port", "1234", "--broker", "coinffeine://host:8181")
    cli.port should be (1234)
    cli.brokerAddress should be (PeerConnection("host", 8181))
  }

  it should "honor the default port" in {
    parseCli("--broker", "coinffeine://host:8181").port should be (PeerCommandLine.DefaultPort)
  }

  it should "reject invalid arguments" in {
    a [ParameterException] should be thrownBy parseCli("--unknown-flag")
  }

  it should "fail if broker address is missing" in {
    a [ParameterException] should be thrownBy parseCli()
  }

  it should "fail if broker address is invalid" in {
    val ex = the [ParameterException] thrownBy parseCli("--broker", "foo:bar:baz")
    ex.toString should include ("Invalid broker address")
  }

  private def parseCli(arguments: String*) = PeerCommandLine.fromArgList(arguments)
}
