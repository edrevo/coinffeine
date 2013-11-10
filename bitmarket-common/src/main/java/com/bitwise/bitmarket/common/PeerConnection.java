package com.bitwise.bitmarket.common;

public class PeerConnection {

    public static final int DEFAULT_PORT = 4790;

    private String hostname;
    private int port;

    public PeerConnection(String hostname) {
        this.hostname = hostname;
        this.port = DEFAULT_PORT;
    }

    public PeerConnection(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("bitmarket://%s:%d/", this.hostname, this.port);
    }

    public String getHostname() { return this.hostname; }

    public int getPort() { return this.port; }
}
