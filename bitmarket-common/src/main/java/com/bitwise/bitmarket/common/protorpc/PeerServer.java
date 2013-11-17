package com.bitwise.bitmarket.common.protorpc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.protobuf.Service;
import com.googlecode.protobuf.pro.duplex.ClientRpcController;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.RpcClient;
import com.googlecode.protobuf.pro.duplex.RpcClientChannel;
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientPipelineFactory;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory;
import com.googlecode.protobuf.pro.duplex.server.RpcClientRegistry;
import com.googlecode.protobuf.pro.duplex.util.RenamingThreadFactoryProxy;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Client and server of a given protobuf service (duplex communication).
 */
public class PeerServer {

    private static final int DEFAULT_NIO_THREADS = 2;
    private static final int CORE_POOL_SIZE = 3;
    private static final int MAXIMUM_POOL_SIZE = 200;

    private final List<EventExecutorGroup> eventGroups;
    private final ThreadPoolCallExecutor executor;
    private final DuplexTcpServerPipelineFactory serverFactory;
    private final DuplexTcpClientPipelineFactory clientFactory;
    private final ServerBootstrap bootstrap;

    public PeerServer(PeerInfo serverInfo, Service service) {
        this.eventGroups = new LinkedList<>();
        this.executor = new ThreadPoolCallExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);

        this.serverFactory = this.makeServerPipelineFactory(serverInfo, service);
        this.bootstrap = makeServerBootstrap(this.serverFactory, serverInfo.getPort());

        this.clientFactory = new DuplexTcpClientPipelineFactory(serverInfo);
        this.clientFactory.setRpcServerCallExecutor(this.executor);
        this.clientFactory.setConnectResponseTimeoutMillis(Constants.CONNECTION_TIMEOUT);
        this.clientFactory.getRpcServiceRegistry().registerService(service);
    }

    private DuplexTcpServerPipelineFactory makeServerPipelineFactory(
            PeerInfo serverInfo, Service service) {
        DuplexTcpServerPipelineFactory factory = new DuplexTcpServerPipelineFactory(serverInfo);
        factory.setRpcServerCallExecutor(this.executor);
        factory.getRpcServiceRegistry().registerService(service);
        return factory;
    }

    private ServerBootstrap makeServerBootstrap(ChannelHandler serverFactory, int port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.namedGroup("boss"), this.namedGroup("worker"));
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(serverFactory);
        bootstrap.localAddress(port);
        bootstrap.option(ChannelOption.SO_SNDBUF, Constants.BUFFER_SIZE);
        bootstrap.option(ChannelOption.SO_RCVBUF, Constants.BUFFER_SIZE);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, Constants.BUFFER_SIZE);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, Constants.BUFFER_SIZE);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        return bootstrap;
    }

    public RpcClientRegistry getClientRegistry() {
        return this.serverFactory.getRpcClientRegistry();
    }

    /**
     * Peer with other server.
     *
     * Don't forget to call #shutdown on the returned object.
     *
     * @param peerInfo  Server to peer with
     * @return          A new session
     * @throws IOException
     */
    public PeerSession peerWith(final PeerInfo peerInfo) throws IOException {
        final DuplexTcpClientPipelineFactory factory = this.clientFactory;
        return new PeerSessionImpl(peerInfo, factory);
    }

    private EventLoopGroup namedGroup(String name) {
        RenamingThreadFactoryProxy threadFactory =
                new RenamingThreadFactoryProxy(name, Executors.defaultThreadFactory());
        EventLoopGroup eventGroup = new NioEventLoopGroup(DEFAULT_NIO_THREADS, threadFactory);
        this.eventGroups.add(eventGroup);
        return eventGroup;
    }

    public void start() {
        this.bootstrap.bind();
    }

    public void shutdown() {
        for (EventExecutorGroup eventGroup : this.eventGroups) {
            eventGroup.shutdownGracefully();
        }
        this.executor.shutdown();
    }

    private static class PeerSessionImpl implements PeerSession {

        private final NioEventLoopGroup eventGroup;
        private final RpcClient channel;
        private final ClientRpcController controller;

        public PeerSessionImpl(
                PeerInfo peerInfo, DuplexTcpClientPipelineFactory clientFactory)
                throws IOException {
            Bootstrap bootstrap = new Bootstrap();
            this.eventGroup = new NioEventLoopGroup();
            bootstrap.group(this.eventGroup);
            bootstrap.handler(clientFactory);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Constants.CONNECTION_TIMEOUT);
            bootstrap.option(ChannelOption.SO_SNDBUF, Constants.BUFFER_SIZE);
            bootstrap.option(ChannelOption.SO_RCVBUF, Constants.BUFFER_SIZE);
            this.channel = clientFactory.peerWith(peerInfo, bootstrap);
            this.controller = this.channel.newRpcController();
        }

        @Override
        public RpcClientChannel getChannel() {
            return this.channel;
        }

        @Override
        public ClientRpcController getController() {
            return this.controller;
        }

        @Override
        public void shutdown() {
            this.channel.close();
            this.eventGroup.shutdownGracefully();
        }
    }
}
