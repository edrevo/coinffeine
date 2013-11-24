package com.bitwise.bitmarket.registry.protorpc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bitwise.bitmarket.common.NetworkTestUtils;
import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf.*;
import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf.PeerService.Stub;
import com.bitwise.bitmarket.common.protorpc.NoopRpc;
import com.bitwise.bitmarket.common.protorpc.PeerServer;
import com.bitwise.bitmarket.common.protorpc.PeerSession;

import static org.junit.Assert.assertEquals;

public class BroadcastServerIT {

    private BroadcastServer instance;
    private TestClient client1;
    private TestClient client2;

    @Before
    public void setUp() throws Exception {
        List<Integer> ports = NetworkTestUtils.findAvailableTcpPorts(3);
        PeerInfo serverInfo = new PeerInfo("localhost", ports.get(0));
        this.instance = new BroadcastServer(serverInfo);
        this.instance.start();
        this.client1 = new TestClient(ports.get(1));
        this.client2 = new TestClient(ports.get(2));
    }

    @After
    public void tearDown() throws Exception {
        this.client2.shutdown();
        this.client1.shutdown();
        this.instance.shutdown();
    }

    @Test
    public void shouldBroadcastAllRequestsToConnectedPeers() throws Exception {
        PublishOffer request1 = PublishOffer.newBuilder()
                .setId(1)
                .setSeq(0)
                .setType(OfferType.BUY)
                .setFrom("???")
                .setConnection(this.client1.info.toString())
                .setAmount(Amount.newBuilder().setValue(100).setScale(2).setCurrency("EUR"))
                .build();
        PublishOffer request2 = request1.toBuilder()
                .setType(OfferType.SELL)
                .build();
        try (PeerSession session1 = this.client1.connectTo(this.instance.getServerInfo());
                PeerSession session2 = this.client2.connectTo(this.instance.getServerInfo())) {
            Stub stub1 = PeerService.newStub(session1.getChannel());
            stub1.publish(session1.getController(), request1, NoopRpc.<VoidResponse>callback());
            Stub stub2 = PeerService.newStub(session2.getChannel());
            stub2.publish(session2.getController(), request2, NoopRpc.<VoidResponse>callback());

            // Keep connections open for the broadcast to happen
            int clientSessionsDuration = 1000;
            Thread.sleep(clientSessionsDuration);
        }
        assertEquals(2, this.client1.getReceivedMessagesNumber());
        assertEquals(2, this.client2.getReceivedMessagesNumber());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenStartedAgain() throws Exception {
        this.instance.start();
    }

    private static class TestClient {

        private final PeerServer server;
        private final MessageCounter messageCounter;
        public final PeerInfo info;

        public TestClient(int port) {
            this.info = new PeerInfo("localhost", port);
            this.messageCounter = new MessageCounter();
            this.server = new PeerServer(
                    this.info, PeerService.newReflectiveService(this.messageCounter));
            this.server.start();
        }

        public int getReceivedMessagesNumber() {
            return this.messageCounter.receivedMessages.get();
        }

        public void shutdown() {
            this.server.shutdown();
        }

        public PeerSession connectTo(PeerInfo serverInfo) throws IOException {
            return this.server.peerWith(serverInfo);
        }
    }

    private static class MessageCounter implements PeerService.Interface {

        public final AtomicInteger receivedMessages = new AtomicInteger();

        @Override
        public void publish(
                RpcController controller, PublishOffer request, RpcCallback<VoidResponse> done) {
            this.receivedMessages.incrementAndGet();
            done.run(VoidResponse.newBuilder().setResult(Result.OK).build());
        }
    }
}
