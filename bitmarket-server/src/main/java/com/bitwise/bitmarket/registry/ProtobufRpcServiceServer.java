package com.bitwise.bitmarket.registry;

import java.util.concurrent.Executors;

import com.google.protobuf.Service;
import com.googlecode.protobuf.pro.duplex.PeerInfo;
import com.googlecode.protobuf.pro.duplex.execute.RpcServerCallExecutor;
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor;
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory;
import com.googlecode.protobuf.pro.duplex.util.RenamingThreadFactoryProxy;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class ProtobufRpcServiceServer {

    private static final int BUFFER_SIZE = 1048576;

    private final ServerBootstrap bootstrap;

    public ProtobufRpcServiceServer(String host, int port, Service service) {
        PeerInfo serverInfo = new PeerInfo(host, port);
        RpcServerCallExecutor executor = new ThreadPoolCallExecutor(3, 200);
        DuplexTcpServerPipelineFactory serverFactory =
                new DuplexTcpServerPipelineFactory(serverInfo);
        serverFactory.setRpcServerCallExecutor(executor);
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(namedGroup("boss"), namedGroup("worker"));
        this.bootstrap.channel(NioServerSocketChannel.class);
        this.bootstrap.childHandler(serverFactory);
        this.bootstrap.localAddress(serverInfo.getPort());
        this.bootstrap.option(ChannelOption.SO_SNDBUF, BUFFER_SIZE);
        this.bootstrap.option(ChannelOption.SO_RCVBUF, BUFFER_SIZE);
        this.bootstrap.childOption(ChannelOption.SO_RCVBUF, BUFFER_SIZE);
        this.bootstrap.childOption(ChannelOption.SO_SNDBUF, BUFFER_SIZE);
        this.bootstrap.option(ChannelOption.TCP_NODELAY, true);
        serverFactory.getRpcServiceRegistry().registerService(service);
    }

    private static NioEventLoopGroup namedGroup(String name) {
        return new NioEventLoopGroup(
                0, new RenamingThreadFactoryProxy(name, Executors.defaultThreadFactory()));
    }

    public void start() {
        this.bootstrap.bind();
    }
}
