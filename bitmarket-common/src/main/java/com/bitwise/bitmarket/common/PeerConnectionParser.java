package com.bitwise.bitmarket.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeerConnectionParser {

    private static final Pattern CONN_PATTERN =
            Pattern.compile("bitmarket://(\\w+(?:\\.\\w+)*)(?::(\\d+))?/?");

    /**
     * Parse a stringfied connection. If connection is not rightly represented, a
     * IllegalArgumentException is thrown.
     *
     * @param conn The stringfied connection to be parsed
     * @return The parsed connection
     * @throws IllegalArgumentException If connection format is invalid
     */
    public static PeerConnection parse(String conn) {
        Matcher m = CONN_PATTERN.matcher(conn);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format(
                    "cannot parse connection chain '%s': invalid format",
                    conn));
        }
        String hostname = m.group(1);
        int port = (m.group(2) != null) ?
                parsePort(conn, m.group(2)) : PeerConnection.DEFAULT_PORT;
        return new PeerConnection(hostname, port);
    }

    private static int parsePort(String conn, String port) {
        try {
            return Integer.valueOf(port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("cannot parse connection chain '%s': invalid port format", conn),
                    e);
        }
    }
}
