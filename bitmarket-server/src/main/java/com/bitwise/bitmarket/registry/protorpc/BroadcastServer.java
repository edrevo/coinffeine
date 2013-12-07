package com.bitwise.bitmarket.registry.protorpc;

import javax.annotation.Nullable;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.server.RpcClientRegistry;

import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf.*;
import com.bitwise.bitmarket.common.protorpc.NoopRpc;
import com.bitwise.bitmarket.common.protorpc.PeerServer;

public class BroadcastServer {

    private static final VoidResponse OK_RESPONSE = VoidResponse.newBuilder()
            .setResult(Result.OK)
            .build();

    private final PeerInfo serverInfo;
    @Nullable
    private BroadcastServiceImpl service;
    @Nullable
    private PeerServer server;

    public BroadcastServer(PeerInfo serverInfo) {
        this.server = null;
        this.service = null;
        this.serverInfo = serverInfo;
    }

    public PeerInfo getServerInfo() {
        return this.serverInfo;
    }

    public void start() {
        if (this.service != null) {
            throw new IllegalStateException("Server already started");
        }
        this.service = new BroadcastServiceImpl();
        this.server = new PeerServer(
                this.serverInfo, BroadcastService.newReflectiveService(this.service));
        this.service.setClientRegistry(this.server.getClientRegistry());
        this.server.start();
    }

    public void shutdown() {
        if (this.server != null) {
            this.server.shutdown();
        }
    }

    private class BroadcastServiceImpl implements BroadcastService.Interface {

        @Nullable
        private RpcClientRegistry clientRegistry = null;

        public void setClientRegistry(RpcClientRegistry clientRegistry) {
            this.clientRegistry = clientRegistry;
        }

        @Override
        public void publish(
                RpcController controller, Offer request, RpcCallback<VoidResponse> done) {
            done.run(OK_RESPONSE);
            if (this.clientRegistry == null) {
                throw new NullPointerException("Missing clientRegistry");
            }
            for (RpcClientChannel channel : this.clientRegistry.getAllClients()) {
                PeerService.Stub peer = PeerService.newStub(channel);
                System.out.println("Republish to " + peer);
                peer.publish(channel.newRpcController(), request, NoopRpc.<VoidResponse>callback());
            }
        }
    }
}
