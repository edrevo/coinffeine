package com.coinffeine.server

import com.beust.jcommander.ParameterException

import com.coinffeine.common.UnitTest

class CommandLineTest extends UnitTest {

  "The command line parser" must "parse the port" in {
    parseCli("--port", "1234").port should be (1234)
  }

  it should "honor the default port" in {
    parseCli().port should be (CommandLine.DefaultPort)
  }

  it should "reject invalid arguments" in {
    a [ParameterException] should be thrownBy parseCli("--unknown-flag")
  }

  private def parseCli(arguments: String*) = CommandLine.fromArgList(arguments)
}
