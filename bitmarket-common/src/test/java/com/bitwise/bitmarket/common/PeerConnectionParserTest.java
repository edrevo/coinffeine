package com.bitwise.bitmarket.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PeerConnectionParserTest {

    @Test
    public void mustParseConnectionChainWithHostnameAndPort() {
        PeerConnection conn = PeerConnectionParser.parse(
                "bitmarket://example.com:9876");
        assertEquals("example.com", conn.getHostname());
        assertEquals(9876, conn.getPort());
    }

    @Test
    public void mustParseConnectionChainWithOnlyHostname() {
        PeerConnection conn = PeerConnectionParser.parse(
                "bitmarket://example.com");
        assertEquals("example.com", conn.getHostname());
        assertEquals(PeerConnection.DEFAULT_PORT, conn.getPort());
    }

    @Test
    public void mustParseConnectionChainWithHostnameAndPortAndTrailingSlash() {
        PeerConnection conn = PeerConnectionParser.parse(
                "bitmarket://example.com:9876/");
        assertEquals("example.com", conn.getHostname());
        assertEquals(9876, conn.getPort());
    }

    @Test
    public void mustParseConnectionChainWithOnlyHostnameAndTrailingSlash() {
        PeerConnection conn = PeerConnectionParser.parse(
                "bitmarket://example.com/");
        assertEquals("example.com", conn.getHostname());
        assertEquals(PeerConnection.DEFAULT_PORT, conn.getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustThrowOnMissingSchemePrefix() {
        PeerConnectionParser.parse("example.com:9876");
    }
}
