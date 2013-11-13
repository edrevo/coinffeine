package com.bitwise.bitmarket.common.protoservices;

import java.util.concurrent.Executors;

import com.google.protobuf.Service;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory;
import com.googlecode.protobuf.pro.duplex.util.RenamingThreadFactoryProxy;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ServiceServer {

    private static final int CORE_POOL_SIZE = 3;
    private static final int MAXIMUM_POOL_SIZE = 200;

    private final ServerBootstrap bootstrap;

    public ServiceServer(PeerInfo serverInfo, Service service) {
        RpcServerCallExecutor executor =
                new ThreadPoolCallExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);
        DuplexTcpServerPipelineFactory serverFactory =
                new DuplexTcpServerPipelineFactory(serverInfo);
        serverFactory.setRpcServerCallExecutor(executor);
        serverFactory.getRpcServiceRegistry().registerService(service);
        this.bootstrap = makeDefaultBootstrap(serverFactory, serverInfo.getPort());
    }

    private static ServerBootstrap makeDefaultBootstrap(ChannelHandler serverFactory, int port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(namedGroup("boss"), namedGroup("worker"));
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

    private static EventLoopGroup namedGroup(String name) {
        return new NioEventLoopGroup(
                0, new RenamingThreadFactoryProxy(name, Executors.defaultThreadFactory()));
    }

    public void start() {
        this.bootstrap.bind();
    }
}
