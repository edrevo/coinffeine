package com.bitwise.bitmarket.common.protorpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.pro.duplex.ClientRpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bitwise.bitmarket.common.NetworkTestUtils;
import com.bitwise.bitmarket.common.protocol.protobuf.TestProtocol.Request;
import com.bitwise.bitmarket.common.protocol.protobuf.TestProtocol.Response;
import com.bitwise.bitmarket.common.protocol.protobuf.TestProtocol.SimpleService;

import static org.junit.Assert.assertEquals;

/**
 * Integration test for PeerServers
 */
public class PeerServerIT {

    private static final Request HELLO_REQUEST = Request.newBuilder().setPayload("hello").build();
    private static final int PEERS_NUMBER = 3;
    private static final long POLL_TIMEOUT = 5;

    private static List<TestPeer> peers;

    @BeforeClass
    public static void setUp() throws Exception {
        peers = new ArrayList<>();
        for (int port : NetworkTestUtils.findAvailableTcpPorts(PEERS_NUMBER)) {
            peers.add(new TestPeer(port));
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        for (TestPeer peer : peers) {
            peer.shutdown();
        }
    }

    @Test
    public void shouldPublishSynchronously() throws Exception {
        TestPeer sender = peers.get(0);
        TestPeer receiver = peers.get(1);
        System.out.println("Peers created...");
        sender.synchronouslyPublishTo(receiver.info);
        System.out.println("Done.");
        receiver.receivedOffers.poll(POLL_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void shouldPublishAsynchronously() throws Exception {
        TestPeer sender = peers.get(0);
        TestPeer receiver = peers.get(2);
        System.out.println("Peers created...");
        sender.asynchronouslyPublishTo(receiver.info);
        System.out.println("Done.");
        receiver.receivedOffers.poll(POLL_TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void shouldBroadcastToConnectedPeers() throws Exception {
        TestPeer connectedPeer = peers.get(0);
        TestPeer broadcastPeer = peers.get(1);
        PeerSession session = connectedPeer.server.peerWith(broadcastPeer.info);
        broadcastPeer.broadcast("hello all");
        String receivedMessage =
                connectedPeer.receivedOffers.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
        session.shutdown();
        assertEquals("hello all", receivedMessage);
    }

    private static class TestPeer {

        final int port;
        final PeerInfo info;
        final PeerServer server;
        final BlockingQueue<String> receivedOffers;

        public TestPeer(int port) {
            this.port = port;
            this.info = new PeerInfo("localhost", port);
            this.receivedOffers = new LinkedBlockingDeque<>();
            this.server = new PeerServer(this.info, SimpleService.newReflectiveService(new Handler()));
            this.server.start();
        }

        public void synchronouslyPublishTo(PeerInfo info) throws IOException, ServiceException {
            System.out.println("Creating client...");
            PeerSession session = this.server.peerWith(info);
            SimpleService.BlockingInterface otherPeer =
                    SimpleService.newBlockingStub(session.getChannel());
            System.out.println("Publishing...");
            otherPeer.greet(session.getController(), HELLO_REQUEST);
            System.out.println("Published.");
            session.shutdown();
            System.out.println("Client done.");
        }

        public void asynchronouslyPublishTo(PeerInfo info)
                throws IOException, ServiceException, InterruptedException {
            System.out.println("Creating client...");
            PeerSession session = this.server.peerWith(info);
            SimpleService.Stub otherPeer = SimpleService.newStub(session.getChannel());
            System.out.println("Publishing...");
            otherPeer.greet(session.getController(), HELLO_REQUEST, NoopRpc.<Response>callback());
            System.out.println("Published.");
            session.shutdown();
            System.out.println("Client done.");
        }

        public void broadcast(String payload) {
            Request request = Request.newBuilder()
                    .setPayload(payload)
                    .build();
            for (RpcClientChannel channel : this.server.getClientRegistry().getAllClients()) {
                ClientRpcController controller = channel.newRpcController();
                SimpleService.Stub connectedPeer = SimpleService.newStub(channel);
                connectedPeer.greet(controller, request, NoopRpc.<Response>callback());
            }
        }

        public void shutdown() {
            this.server.shutdown();
        }

        private class Handler implements SimpleService.Interface {

            @Override
            public void greet(
                    RpcController controller, Request request, RpcCallback<Response> done) {
                System.out.printf("'%s' received.%n", request.getPayload());
                TestPeer.this.receivedOffers.add(request.getPayload());
                done.run(Response.newBuilder().setCode(0).build());
            }
        }
    }
}
