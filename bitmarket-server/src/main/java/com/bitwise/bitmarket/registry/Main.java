package com.bitwise.bitmarket.registry;

import com.google.protobuf.Service;

import com.bitwise.bitmarket.common.protocol.Offer.RegistryService;

/**
 * Testing entry point
 */
public class Main {

    public static void main(String[] args) {
        InMemoryRegistryImpl serviceImplementation = new InMemoryRegistryImpl();
        Service service = RegistryService.newReflectiveService(serviceImplementation);
        ProtobufRpcServiceServer server = new ProtobufRpcServiceServer("localhost", 8080, service);
        server.start();
        System.out.println("Listening...");
    }
}
