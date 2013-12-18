package com.bitwise.bitmarket.registry.protorpc;

import javax.annotation.Nullable;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.server.RpcClientRegistry;
import io.netty.channel.ChannelFuture;

import com.bitwise.bitmarket.common.protocol.protobuf.BitmarketProtobuf;
import com.bitwise.bitmarket.common.protorpc.NoopRpc;
import com.bitwise.bitmarket.common.protorpc.PeerServer;

public class BroadcastServer {

    private static final BitmarketProtobuf.PublishResponse SUCCESS_RESPONSE =
            BitmarketProtobuf.PublishResponse.newBuilder()
                    .setResult(BitmarketProtobuf.PublishResponse.Result.SUCCESS)
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

    public ChannelFuture start() {
        if (this.service != null) {
            throw new IllegalStateException("Server already started");
        }
        this.service = new BroadcastServiceImpl();
        this.server = new PeerServer(
                this.serverInfo,
                BitmarketProtobuf.BroadcastService.newReflectiveService(this.service));
        this.service.setClientRegistry(this.server.getClientRegistry());
        return this.server.start();
    }

    public void shutdown() {
        if (this.server != null) {
            this.server.shutdown();
        }
    }

    private class BroadcastServiceImpl implements BitmarketProtobuf.BroadcastService.Interface {

        @Nullable
        private RpcClientRegistry clientRegistry = null;

        public void setClientRegistry(RpcClientRegistry clientRegistry) {
            this.clientRegistry = clientRegistry;
        }

        @Override
        public void publish(
                RpcController controller,
                BitmarketProtobuf.Offer request,
                RpcCallback<BitmarketProtobuf.PublishResponse> done) {
            done.run(SUCCESS_RESPONSE);
            if (this.clientRegistry == null) {
                throw new NullPointerException("Missing clientRegistry");
            }
            for (RpcClientChannel channel : this.clientRegistry.getAllClients()) {
                BitmarketProtobuf.PeerService.Stub peer = BitmarketProtobuf.PeerService.newStub(
                        channel);
                System.out.println("Republish to " + peer);
                peer.publish(
                        channel.newRpcController(),
                        request,
                        NoopRpc.<BitmarketProtobuf.PublishResponse>callback());
            }
        }
    }
}
