package com.bitwise.bitmarket.registry;

import com.beust.jcommander.ParameterException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommandLineTest {

    @Test
    public void shouldParseValidCommandLine() throws Exception {
        CommandLine cli = parseCli("--port", "1234");
        assertEquals(1234, cli.port);
    }

    @Test
    public void shouldHonorDefaultPort() throws Exception {
        CommandLine cli = parseCli();
        assertEquals(CommandLine.DEFAULT_PORT, cli.port);
    }

    @Test(expected = ParameterException.class)
    public void shouldRejectInvalidArguments() throws Exception {
        parseCli("--unknown-flag");
    }

    private CommandLine parseCli(String... arguments) {
        return CommandLine.fromArgList(arguments);
    }
}
