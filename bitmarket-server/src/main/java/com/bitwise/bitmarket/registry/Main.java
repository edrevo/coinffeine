package com.bitwise.bitmarket.registry;

import java.io.IOException;

import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;

import com.bitwise.bitmarket.registry.protorpc.BroadcastServer;

public class Main {

    public static void main(String[] args) throws IOException, ServiceException {
        PeerInfo serverInfo = new PeerInfo("localhost", 8080);
        BroadcastServer server = new BroadcastServer(serverInfo);
        server.start();
        System.out.println("Listening...");
    }
}
