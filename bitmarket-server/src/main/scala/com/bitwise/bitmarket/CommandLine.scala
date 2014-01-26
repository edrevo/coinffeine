package com.bitwise.bitmarket

import com.beust.jcommander.{JCommander, Parameter}

class CommandLine {
  @Parameter(names = Array("-p", "--port"))
  var port: Int = CommandLine.DefaultPort
}

object CommandLine {

  val DefaultPort = 8080

  def fromArgList(args: Seq[String]): CommandLine = {
    val cli = new CommandLine
    new JCommander(cli, args: _*)
    cli
  }
}
