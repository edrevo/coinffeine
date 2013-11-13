package com.bitwise.bitmarket.common.protoservices;

import java.io.IOException;
import java.net.InetAddress;

import com.google.protobuf.RpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientPipelineFactory;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import com.bitwise.bitmarket.common.PeerConnection;

public abstract class ServiceClient<ServiceInterface> {

    private static final int CORE_POOL_SIZE = 3;
    private static final int MAXIMUM_POOL_SIZE = 100;

    private final ServiceInterface registryService;
    private final RpcController controller;
    private final RpcClientChannel channel;

    public ServiceClient(PeerInfo serverInfo) throws IOException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        PeerInfo clientInfo = new PeerInfo(hostAddress, PeerConnection.DEFAULT_PORT);
        DuplexTcpClientPipelineFactory clientFactory =
                new DuplexTcpClientPipelineFactory(clientInfo);
        this.enableDuplexCommunication(clientFactory);
        Bootstrap bootstrap = this.makeDefaultBootstrap(clientFactory);
        this.channel = clientFactory.peerWith(serverInfo, bootstrap);
        this.registryService = buildService(this.channel);
        this.controller = this.channel.newRpcController();
    }

    private void enableDuplexCommunication(DuplexTcpClientPipelineFactory clientFactory) {
        RpcServerCallExecutor executor =
                new ThreadPoolCallExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);
        clientFactory.setRpcServerCallExecutor(executor);
        clientFactory.setConnectResponseTimeoutMillis(Constants.CONNECTION_TIMEOUT);
    }

    private Bootstrap makeDefaultBootstrap(ChannelHandler clientFactory) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup());
        bootstrap.handler(clientFactory);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Constants.CONNECTION_TIMEOUT);
        bootstrap.option(ChannelOption.SO_SNDBUF, Constants.BUFFER_SIZE);
        bootstrap.option(ChannelOption.SO_RCVBUF, Constants.BUFFER_SIZE);
        return bootstrap;
    }

    protected abstract ServiceInterface buildService(RpcClientChannel channel);

    public RpcController getController() {
        return this.controller;
    }

    public ServiceInterface getService() {
        return this.registryService;
    }

    public void close() {
        this.channel.close();
    }
}
