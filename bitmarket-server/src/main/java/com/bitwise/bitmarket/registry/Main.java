package com.bitwise.bitmarket.registry;

import java.io.IOException;

import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitwise.bitmarket.registry.protorpc.BroadcastServer;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ServiceException {
        CommandLine cli = CommandLine.fromArgList(args);
        PeerInfo serverInfo = new PeerInfo("localhost", cli.port);
        final BroadcastServer server = new BroadcastServer(serverInfo);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("Gracefully shutting down the server...");
                server.shutdown();
                LOGGER.info("Resources were freed.");
            }
        });
        server.start();
        LOGGER.info("Listening on {}:{}...\n", serverInfo.getHostName(), serverInfo.getPort());
    }
}
