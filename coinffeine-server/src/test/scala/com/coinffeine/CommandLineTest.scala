package com.coinffeine

import com.beust.jcommander.ParameterException
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class CommandLineTest extends FlatSpec with ShouldMatchers {

  "The command line parser" must "parse the port" in {
    parseCli("--port", "1234").port should be (1234)
  }

  it should "honor the default port" in {
    parseCli().port should be (CommandLine.DefaultPort)
  }

  it should "reject invalid arguments" in {
    evaluating(parseCli("--unknown-flag")) should produce [ParameterException]
  }

  private def parseCli(arguments: String*) = CommandLine.fromArgList(arguments)
}
