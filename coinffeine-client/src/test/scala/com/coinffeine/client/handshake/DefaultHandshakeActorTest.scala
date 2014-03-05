package com.coinffeine.client.handshake

import scala.language.postfixOps
import scala.util.{Failure, Success}

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.core.{ECKey, Transaction}
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.params.TestNet3Params
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.Exchange
import com.coinffeine.common.{PeerConnection, AkkaSpec}
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, ForwardMessage}
import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protocol.messages.handshake.{RefundTxSignatureResponse, RefundTxSignatureRequest}

/** Test fixture for testing the handshake actor interaction, one derived class per scenario. */
abstract class DefaultHandshakeActorTest(systemName: String)
  extends AkkaSpec(systemName) with MockitoSugar {

  class MockExchangeHandshake extends ExchangeHandshake {
    override val exchange = Exchange(
      "id",
      PeerConnection("counterpart"),
      PeerConnection("broker"),
      network = TestNet3Params.get(),
      userKey = new ECKey(),
      counterpartKey = null,
      exchangeAmount = BtcAmount(10),
      steps = 10,
      lockTime = 10)
    override val commitmentTransaction = MockTransaction()
    override val refundTransaction = MockTransaction()
    val counterpartCommitmentTransaction = MockTransaction()
    val counterpartRefund = MockTransaction()
    val invalidRefundTransaction = MockTransaction()

    val refundSignature = mock[TransactionSignature]
    val counterpartRefundSignature = mock[TransactionSignature]

    override def signCounterpartRefundTransaction(txToSign: Transaction) =
      if (txToSign == counterpartRefund) Success(counterpartRefundSignature)
      else Failure(new Error("Invalid refundSig"))

    override def validateRefundSignature(sig: TransactionSignature) =
      if (sig == refundSignature) Success(()) else Failure(new Error("Invalid signature!"))
  }

  def protocolConstants: ProtocolConstants

  val handshake = new MockExchangeHandshake
  val listener = TestProbe()
  val gateway = TestProbe()
  val blockchain = TestProbe()
  val transactionSerialization = new FakeTransactionSerialization(
    transactions = Seq(
      handshake.commitmentTransaction,
      handshake.refundTransaction,
      handshake.counterpartCommitmentTransaction,
      handshake.counterpartRefund,
      handshake.invalidRefundTransaction
    ),
    signatures = Seq.empty
  )
  val actor = system.actorOf(Props(new DefaultHandshakeActor(handshake,
    gateway.ref, blockchain.ref, transactionSerialization, protocolConstants, Seq(listener.ref))
  ), "handshake-actor")
  listener.watch(actor)

  def shouldForwardRefundSignatureRequest() {
    shouldForwardToCounterpart(
      RefundTxSignatureRequest("id", handshake.refundTransaction.bitcoinSerialize()))
  }

  def shouldSignCounterpartRefund() {
    val request = RefundTxSignatureRequest("id", handshake.counterpartRefund.bitcoinSerialize())
    gateway.send(actor, ReceiveMessage(request, handshake.exchange.counterpart))
    shouldForwardToCounterpart(
      RefundTxSignatureResponse("id", handshake.counterpartRefundSignature))
  }

  def shouldForwardToCounterpart[T : MessageSend](message: T) {
    gateway.expectMsg(ForwardMessage(message, handshake.exchange.counterpart))
  }

  def shouldForwardToBroker[T : MessageSend](message: T) {
    gateway.expectMsg(ForwardMessage(message, handshake.exchange.broker))
  }

  def fromCounterpart(message: Any) = ReceiveMessage(message, handshake.exchange.counterpart)

  def fromBroker(message: Any) = ReceiveMessage(message, handshake.exchange.broker)
}
