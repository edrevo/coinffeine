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
import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf;
import com.bitwise.bitmarket.common.protorpc.NoopRpc;
import com.bitwise.bitmarket.common.protorpc.PeerServer;
import com.bitwise.bitmarket.common.protorpc.PeerSession;

import static com.bitwise.bitmarket.common.ConcurrentAssert.assertEventually;
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
        this.instance.start().await();
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
        BitmarketProtobuf.Offer request1 = BitmarketProtobuf.Offer.newBuilder()
                .setId(1)
                .setSeq(0)
                .setFrom("???")
                .setConnection(this.client1.info.toString())
                .setAmount(BitmarketProtobuf.BtcAmount.newBuilder()
                        .setValue(100)
                        .setScale(2))
                .setBtcPrice(BitmarketProtobuf.FiatAmount.newBuilder()
                        .setValue(100)
                        .setScale(2)
                        .setCurrency("EUR"))
                .build();
        BitmarketProtobuf.Offer request2 = request1.toBuilder()
                .build();
        try (PeerSession session1 = this.client1.connectTo(this.instance.getServerInfo());
                PeerSession session2 = this.client2.connectTo(this.instance.getServerInfo())) {
            BitmarketProtobuf.BroadcastService.Stub stub1 =
                    BitmarketProtobuf.BroadcastService.newStub(session1.getChannel());
            stub1.publish(
                    session1.getController(),
                    request1,
                    NoopRpc.<BitmarketProtobuf.PublishResponse>callback());
            BitmarketProtobuf.BroadcastService.Stub stub2 =
                    BitmarketProtobuf.BroadcastService.newStub(session2.getChannel());
            stub2.publish(
                    session2.getController(),
                    request2,
                    NoopRpc.<BitmarketProtobuf.PublishResponse>callback());

            assertEventually(new Runnable() {
                @Override
                public void run() {
                    assertEquals(2, BroadcastServerIT.this.client1.getReceivedMessagesNumber());
                    assertEquals(2, BroadcastServerIT.this.client2.getReceivedMessagesNumber());
                }
            });
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenStartedAgain() throws Exception {
        this.instance.start();
    }

    private static class TestClient {

        private final PeerServer server;
        private final MessageCounter messageCounter;
        public final PeerInfo info;

        public TestClient(int port) throws InterruptedException {
            this.info = new PeerInfo("localhost", port);
            this.messageCounter = new MessageCounter();
            this.server = new PeerServer(
                    this.info,
                    BitmarketProtobuf.PeerService.newReflectiveService(this.messageCounter));
            this.server.start().await();
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

    private static class MessageCounter implements BitmarketProtobuf.PeerService.Interface {

        public final AtomicInteger receivedMessages = new AtomicInteger();

        @Override
        public void publish(
                RpcController controller,
                BitmarketProtobuf.Offer request,
                RpcCallback<BitmarketProtobuf.PublishResponse> done) {
            this.receivedMessages.incrementAndGet();
            done.run(BitmarketProtobuf.PublishResponse.newBuilder()
                    .setResult(BitmarketProtobuf.PublishResponse.Result.SUCCESS)
                    .build());
        }

        @Override
        public void requestExchange(
                RpcController controller,
                BitmarketProtobuf.ExchangeRequest request,
                RpcCallback<BitmarketProtobuf.ExchangeRequestResponse> done) {
            throw new UnsupportedOperationException();
        }
    }
}
