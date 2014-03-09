package com.coinffeine.client.peer

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.beust.jcommander.ParameterException
import com.coinffeine.common.PeerConnection

class PeerCommandLineTest extends FlatSpec with ShouldMatchers {

  "The command line parser" must "parse local port and broker address" in {
    val cli = parseCli("--port", "1234", "--broker", "coinffeine://host:8181")
    cli.port should be (1234)
    cli.brokerAddress should be (PeerConnection("host", 8181))
  }

  it should "honor the default port" in {
    parseCli("--broker", "coinffeine://host:8181").port should be (PeerCommandLine.DefaultPort)
  }

  it should "reject invalid arguments" in {
    evaluating(parseCli("--unknown-flag")) should produce [ParameterException]
  }

  it should "fail if broker address is missing" in {
    evaluating(parseCli()) should produce [ParameterException]
  }

  it should "fail if broker address is invalid" in {
    val ex = evaluating {
      parseCli("--broker", "foo:bar:baz")
    } should produce [ParameterException]
    ex.toString should include ("Invalid broker address")
  }

  private def parseCli(arguments: String*) = PeerCommandLine.fromArgList(arguments)
}
