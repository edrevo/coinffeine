package com.bitwise.bitmarket.registry;

import java.io.IOException;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;

import com.bitwise.bitmarket.common.currency.CurrencyCode;
import com.bitwise.bitmarket.common.protocol.protobuf.OfferProtocol.*;
import com.bitwise.bitmarket.common.protocol.protobuf.OfferProtocol.PeerService.BlockingInterface;
import com.bitwise.bitmarket.common.protorpc.ServiceClient;
import com.bitwise.bitmarket.registry.protorpc.BroadcastServer;

/**
 * Testing entry point
 */
public class Main {

    public static void main(String[] args) throws IOException, ServiceException {
        PeerInfo serverInfo = new PeerInfo("localhost", 8080);
        BroadcastServer server = new BroadcastServer(serverInfo);
        server.start();
        System.out.println("Listening...");
        sendTestMessages();
    }

    private static void sendTestMessages() throws IOException, ServiceException {
        Service serviceImplementation = PeerService.newReflectiveService(
                new PeerService.Interface() {
                    @Override
                    public void publish(
                            RpcController controller, PublishOffer request,
                            RpcCallback<VoidResponse> done) {
                        System.out.println("Message shouted back! " + request);
                        done.run(VoidResponse.getDefaultInstance());
                    }
                });

        ServiceClient<BlockingInterface> client =
                new ServiceClient<BlockingInterface>(new PeerInfo("localhost", 8080), serviceImplementation) {
                    @Override
                    protected BlockingInterface buildService(RpcClientChannel channel) {
                        return PeerService.newBlockingStub(channel);
                    }
                };


        System.out.println("Sending publication...");
        VoidResponse pubRes =
                client.getService().publish(
                        client.getController(), PublishOffer.newBuilder()
                        .setAmount(Amount.newBuilder()
                                .setValue(10)
                                .setScale(0)
                                .setCurrency(CurrencyCode.EUR.toString())
                                .build())
                        .setConnection("localhost:1234")
                        .setFrom("localhost:1234")
                        .setId(42)
                        .setType(OfferType.BUY)
                        .build());
        System.out.println("Publish response: " + pubRes);

        client.close();
    }
}
