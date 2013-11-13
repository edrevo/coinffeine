package com.bitwise.bitmarket.registry;

import java.io.IOException;

import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;

import com.bitwise.bitmarket.common.protocol.Offer.*;
import com.bitwise.bitmarket.common.protocol.Offer.RegistryService.BlockingInterface;
import com.bitwise.bitmarket.common.protoservices.ServiceClient;
import com.bitwise.bitmarket.common.protoservices.ServiceServer;

/**
 * Testing entry point
 */
public class Main {

    public static void main(String[] args) throws IOException, ServiceException {
        InMemoryRegistryImpl serviceImplementation = new InMemoryRegistryImpl();
        Service service = RegistryService.newReflectiveService(serviceImplementation);
        PeerInfo serverInfo = new PeerInfo("localhost", 8080);
        ServiceServer server = new ServiceServer(serverInfo, service);
        server.start();
        System.out.println("Listening...");
        sendTestMessages();
    }

    private static void sendTestMessages() throws IOException, ServiceException {
        ServiceClient<BlockingInterface> client =
                new ServiceClient<BlockingInterface>(new PeerInfo("localhost", 8080)) {
                    @Override
                    protected BlockingInterface buildService(RpcClientChannel channel) {
                        return RegistryService.newBlockingStub(channel);
                    }
                };

        VoidResponse regRes = client.getService().registerClient(
                client.getController(),
                RegistrationRequest.newBuilder().setConnection("localhost:1234").build());
        System.out.println("Registration response: " + regRes);

        VoidResponse pubRes =
                client.getService().publish(
                        client.getController(), PublishOffer.newBuilder()
                        .setAmount(10)
                        .setConnection("localhost:1234")
                        .setFrom("localhost:1234")
                        .setId(42)
                        .setType(OfferType.BUY)
                        .build());
        System.out.println("Publish response: " + pubRes);

        client.close();
    }
}
