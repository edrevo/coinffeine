package com.bitwise.bitmarket.registry;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class CommandLine {

    public static final int DEFAULT_PORT = 8080;

    @Parameter(
            names = { "-p", "--port" },
            description = "Port to use. Default value: " + DEFAULT_PORT
    )
    public int port = DEFAULT_PORT;

    public static CommandLine fromArgList(String[] args) {
        CommandLine cli = new CommandLine();
        new JCommander(cli, args);
        return cli;
    }
}
