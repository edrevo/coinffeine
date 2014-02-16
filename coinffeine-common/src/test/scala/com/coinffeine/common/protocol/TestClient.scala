package com.coinffeine.common.protocol

import java.util.Currency
import scala.concurrent._

import com.google.protobuf.{Message => ProtoMessage, RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.common.protocol.protobuf.{CoinffeineProtobuf => msg}
import com.coinffeine.common.protorpc.{PeerSession, PeerServer}
import com.coinffeine.common.PeerConnection

class TestClient(port: Int, serverInfo: PeerInfo) extends msg.PeerService.Interface {
  val info = new PeerInfo("localhost", port)
  val connection = PeerConnection(info.getHostName, port)
  var sessionOption: Option[PeerSession] = None

  @volatile
  private var receivedMessages_ : Seq[ProtoMessage] = Seq.empty
  private val server = new PeerServer(info, msg.PeerService.newReflectiveService(this))

  server.start().await

  def receivedMessages: Seq[ProtoMessage] = synchronized { receivedMessages_ }

  def receivedMessagesNumber: Int = receivedMessages.size

  def shutdown() {
    disconnect()
    server.shutdown()
  }

  def connectToServer() {
    sessionOption = Some(server.peerWith(serverInfo).get)
  }

  def disconnect() {
    sessionOption.foreach(_.close())
    sessionOption = None
  }

  def placeOrder(order: msg.Order): msg.OrderResponse = {
    val session = sessionOption.get
    val stub = msg.BrokerService.newBlockingStub(session.channel)
    stub.placeOrder(session.controller, order)
  }

  def requestQuote(currency: Currency): Future[msg.QuoteResponse] = {
    val session = sessionOption.get
    val stub = msg.BrokerService.newStub(session.channel)
    val request = msg.QuoteRequest.newBuilder.setCurrency(currency.getCurrencyCode).build()
    val promise = Promise[msg.QuoteResponse]()
    stub.requestQuote(session.controller, request, new RpcCallback[msg.QuoteResponse] {
      override def run(response: msg.QuoteResponse) { promise.success(response) }
    })
    promise.future
  }

  def notifyOrderMatch(request: msg.OrderMatch): Future[msg.Void] = {
    val session = sessionOption.get
    val stub = msg.PeerService.newStub(session.channel)
    val promise = Promise[msg.Void]()
    stub.notifyMatch(session.controller, request, new RpcCallback[msg.Void] {
      def run(parameter: msg.Void) { promise.success(msg.Void.newBuilder().build())}
    })
    promise.future
  }

  override def notifyMatch(c: RpcController, request: msg.OrderMatch, done: RpcCallback[msg.Void]) {
    synchronized {
      receivedMessages_ = receivedMessages_ :+ request
    }
    done.run(msg.Void.getDefaultInstance)
  }

  override def submitTxRefundSignature(
      c: RpcController,
      request: msg.RefundTxSignatureResponse,
      done: RpcCallback[msg.Void]): Unit = ???

  override def requestTxRefundSignature(
      c: RpcController,
      request: msg.RefundTxSignatureRequest,
      done: RpcCallback[msg.Void]): Unit = ???

  override def rejectExchange(
      c: RpcController,
      request: msg.ExchangeRejection,
      done: RpcCallback[msg.Void]): Unit = ???

  override def notifyCommitment(
      c: RpcController,
      request: msg.CommitmentNotification,
      done: RpcCallback[msg.Void]): Unit = ???

  override def beginExchange(
      c: RpcController,
      request: msg.EnterExchange,
      done: RpcCallback[msg.Void]): Unit = ???

  override def abortExchange(
      c: RpcController,
      request: msg.ExchangeAborted,
      done: RpcCallback[msg.Void]): Unit = ???
}
