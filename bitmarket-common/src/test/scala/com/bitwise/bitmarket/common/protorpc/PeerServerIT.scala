package com.bitwise.bitmarket.common.protorpc

import java.util.concurrent.{LinkedBlockingDeque, BlockingQueue, TimeUnit}
import scala.collection.JavaConversions._

import com.bitwise.bitmarket.common.NetworkTestUtils
import com.bitwise.bitmarket.common.protocol.protobuf.TestProtocol
import com.bitwise.bitmarket.common.protocol.protobuf.TestProtocol.{Response, SimpleService, Request}
import com.google.protobuf.{RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.{RpcClientChannel, PeerInfo}
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class PeerServerIT extends FlatSpec with ShouldMatchers {

  import PeerServerIT._

  "Peer server" must "publish synchronously" in new SamplePeers {
    withPeers { peers =>
      val sender = peers.get(0)
      val receiver = peers.get(1)
      sender.synchronouslyPublishTo(receiver.info)
      receiver.receivedOffers.poll(pollTimeout, TimeUnit.MILLISECONDS)
    }
  }

  it must "publish asynchronously" in new SamplePeers {
    withPeers { peers =>
      val sender = peers.get(0)
      val receiver = peers.get(2)
      sender.asynchronouslyPublishTo(receiver.info)
      receiver.receivedOffers.poll(pollTimeout, TimeUnit.MILLISECONDS)
    }
  }

  it must "broadcast to connected peers" in new SamplePeers {
    withPeers { peers =>
      val connectedPeer = peers.get(0)
      val broadcastPeer = peers.get(1)
      val session = connectedPeer.server.peerWith(broadcastPeer.info).get
      broadcastPeer.broadcast("hello all")
      val receivedMsg = connectedPeer.receivedOffers.poll(pollTimeout, TimeUnit.MILLISECONDS)
      session.close()
      receivedMsg should be ("hello all")
    }
  }

  private trait SamplePeers {
    val peerNumbers = 3
    val pollTimeout = 500

    def withPeers(action: Seq[TestPeer] => Unit) {
      val peers: Seq[TestPeer] =
        NetworkTestUtils.findAvailableTcpPorts(peerNumbers).map(new TestPeer(_))
      action(peers)
      peers.foreach(_.shutdown())
    }
  }

  private class TestPeer(port: Int) {

    val info = new PeerInfo("localhost", port)
    var receivedOffers: BlockingQueue[String] = new LinkedBlockingDeque[String]()
    val server = {
      val s = new PeerServer(info, SimpleService.newReflectiveService(new Handler))
      s.start().await()
      s
    }

    def synchronouslyPublishTo(peer: PeerInfo) {
      val session = server.peerWith(info).get
      val otherPeer = SimpleService.newBlockingStub(session.channel)
      otherPeer.greet(session.controller, HelloRequest)
      session.close()
    }

    def asynchronouslyPublishTo(peer: PeerInfo) {
      val session = this.server.peerWith(info).get
      val otherPeer = SimpleService.newStub(session.channel)
      otherPeer.greet(
        session.controller, HelloRequest, Callbacks.noop[TestProtocol.Response])
      session.close()
    }

    def broadcast(payload: String) {
      val request = Request.newBuilder.setPayload(payload).build
      val clients: Seq[RpcClientChannel] = server.clientRegistry.getAllClients
      clients.foreach { channel =>
        val controller = channel.newRpcController
        val connectedPeer = SimpleService.newStub(channel)
        connectedPeer.greet(controller, request, Callbacks.noop[TestProtocol.Response])
      }
    }

    def shutdown() { server.shutdown() }

    private class Handler extends SimpleService.Interface {

      override def greet(controller: RpcController, request: Request, done: RpcCallback[Response]) {
        TestPeer.this.receivedOffers.add(request.getPayload)
        done.run(Response.newBuilder().setCode(0).build())
      }
    }
  }
}

object PeerServerIT {
  val HelloRequest = Request.newBuilder().setPayload("hello").build()
}
