package com.bitwise.bitmarket.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PeerConnectionTest {

    @Test
    public void mustInitDefaultPortWhenUnspecified() {
        PeerConnection conn = new PeerConnection("example.com");
        assertEquals(PeerConnection.DEFAULT_PORT, conn.getPort());
    }
}
