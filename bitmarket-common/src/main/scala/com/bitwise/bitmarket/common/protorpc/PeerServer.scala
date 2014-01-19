package com.bitwise.bitmarket.common.protorpc

import java.util.concurrent.Executors
import scala.util.Try

import com.google.protobuf.Service
import com.googlecode.protobuf.pro.duplex.PeerInfo
import com.googlecode.protobuf.pro.duplex.client.DuplexTcpClientPipelineFactory
import com.googlecode.protobuf.pro.duplex.execute.ThreadPoolCallExecutor
import com.googlecode.protobuf.pro.duplex.server.DuplexTcpServerPipelineFactory
import com.googlecode.protobuf.pro.duplex.util.RenamingThreadFactoryProxy
import io.netty.util.concurrent.EventExecutorGroup
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.socket.nio.{NioSocketChannel, NioServerSocketChannel}
import io.netty.channel.{ChannelFuture, EventLoopGroup, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup

class PeerServer(serverInfo: PeerInfo, service: Service) {

  import PeerServer._

  var eventGroups: Seq[EventExecutorGroup] = Seq.empty
  val executor = new ThreadPoolCallExecutor(CorePoolSize, MaxPoolSize)
  val serverFactory  = {
    val factory = new DuplexTcpServerPipelineFactory(serverInfo)
    factory.setRpcServerCallExecutor(executor)
    factory.getRpcServiceRegistry.registerService(service)
    factory
  }
  val bootstrap = {
    val bootstrap = new ServerBootstrap
    bootstrap.group(this.namedGroup("boss"), this.namedGroup("worker"))
    bootstrap.channel(classOf[NioServerSocketChannel])
    bootstrap.childHandler(serverFactory)
    bootstrap.localAddress(serverInfo.getPort)
    bootstrap.option[Integer](ChannelOption.SO_SNDBUF, BufferSize)
    bootstrap.option[Integer](ChannelOption.SO_RCVBUF, BufferSize)
    bootstrap.childOption[Integer](ChannelOption.SO_RCVBUF, BufferSize)
    bootstrap.childOption[Integer](ChannelOption.SO_SNDBUF, BufferSize)
    bootstrap.option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
    bootstrap
  }

  val clientFactory = {
    val fact = new DuplexTcpClientPipelineFactory(serverInfo)
    fact.setRpcServerCallExecutor(executor)
    fact.setConnectResponseTimeoutMillis(ConnectionTimeout)
    fact.getRpcServiceRegistry.registerService(service)
    fact
  }

  def clientRegistry = serverFactory.getRpcClientRegistry

  def peerWith(peer: PeerInfo): Try[PeerSession] = Try(new PeerSessionImpl(peer, clientFactory))

  def start(): ChannelFuture = { bootstrap.bind() }

  def shutdown() {
    eventGroups.foreach(_.shutdownGracefully())
    executor.shutdown()
  }

  private def namedGroup(name: String): EventLoopGroup = {
    val threadFactory = new RenamingThreadFactoryProxy(name, Executors.defaultThreadFactory)
    val eventGroup = new NioEventLoopGroup(DefaultNioThreads, threadFactory)
    eventGroups :+= eventGroup
    eventGroup
  }
}

private [protorpc] class PeerSessionImpl(
    peer: PeerInfo, clientFactory: DuplexTcpClientPipelineFactory) extends PeerSession {

  private val eventGroup = new NioEventLoopGroup()

  private val bootstrap = {
    val bs = new Bootstrap()
    bs.group(eventGroup)
    bs.handler(clientFactory)
    bs.channel(classOf[NioSocketChannel])
    bs.option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
    bs.option[Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, ConnectionTimeout)
    bs.option[Integer](ChannelOption.SO_SNDBUF, BufferSize)
    bs.option[Integer](ChannelOption.SO_RCVBUF, BufferSize)
    bs
  }

  override val channel = clientFactory.peerWith(peer, bootstrap)

  override val controller = channel.newRpcController()

  def close() {
    channel.close()
    eventGroup.shutdownGracefully()
  }
}

object PeerServer {
  val DefaultNioThreads = 2
  val CorePoolSize = 3
  val MaxPoolSize = 200
}
