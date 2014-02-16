package com.coinffeine.common;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;

/**
 * Utilities for integration tests using network facilities.
 */
public class NetworkTestUtils {

    /**
     * Find a number of free TCP ports.
     *
     * Ports are found by opening probe server sockets.
     *
     * @param n Number of ports to find
     * @return  List of available ports
     * @throws RuntimeException When probe server sockets cannot be created or closed.
     */
    public static List<Integer> findAvailableTcpPorts(int n) {
        List<ServerSocket> successfulSockets = new LinkedList<>();
        while (successfulSockets.size() < n) {
            try {
                ServerSocket socket = new ServerSocket(0);
                successfulSockets.add(socket);
            } catch (Exception ex) {
                throw new RuntimeException("Cannot open probe socket", ex);
            }
        }
        List<Integer> availablePorts = new LinkedList<>();
        for (ServerSocket socket : successfulSockets) {
            availablePorts.add(socket.getLocalPort());
            try {
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException("Cannot close probe server socket", ex);
            }
        }
        return availablePorts;
    }
}
