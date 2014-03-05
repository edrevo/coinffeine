package com.coinffeine.client.handshake

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

import com.google.bitcoin.core.Sha256Hash
import com.google.bitcoin.crypto.TransactionSignature

import com.coinffeine.client.handshake.HandshakeActor.HandshakeResult
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.CommitmentNotification
import com.coinffeine.common.protocol.messages.handshake._

class HappyPathDefaultHandshakeActorTest extends DefaultHandshakeActorTest("happy-path") {

  override def protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute
  )

  "Handshake happy path" should "start with a subscription to the relevant messages" in {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val relevantSignatureRequest = RefundTxSignatureRequest(
      "id", handshake.counterpartRefund.bitcoinSerialize())
    val irrelevantSignatureRequest =
      RefundTxSignatureRequest("other-id", handshake.counterpartRefund.bitcoinSerialize())
    filter(fromCounterpart(relevantSignatureRequest)) should be (true)
    filter(ReceiveMessage(relevantSignatureRequest, PeerConnection("other"))) should be (false)
    filter(fromCounterpart(irrelevantSignatureRequest)) should be (false)
    filter(fromCounterpart(RefundTxSignatureResponse("id", handshake.refundSignature))) should be (true)
    filter(fromBroker(CommitmentNotification("id", mock[Sha256Hash], mock[Sha256Hash]))) should be (true)
    filter(fromBroker(ExchangeAborted("id", "failed"))) should be (true)
    filter(fromCounterpart(ExchangeAborted("id", "failed"))) should be (false)
    filter(fromBroker(ExchangeAborted("other", "failed"))) should be (false)
  }

  it should "and requesting refund transaction signature" in {
    shouldForwardRefundSignatureRequest()
  }

  it should "reject signature of invalid counterpart refund transactions" in {
    val invalidRequest = RefundTxSignatureRequest(
      "id", handshake.invalidRefundTransaction.bitcoinSerialize())
    gateway.send(actor, ReceiveMessage(invalidRequest, handshake.exchange.counterpart))
    gateway.expectNoMsg(100 millis)
  }

  it should "sign counterpart refund while waiting for our refund" in {
    shouldSignCounterpartRefund()
  }

  it should "don't be fooled by invalid refund TX or source and resubmit signature request" in {
    gateway.send(actor, fromCounterpart(RefundTxSignatureResponse("id", mock[TransactionSignature])))
    shouldForwardRefundSignatureRequest()
  }

  it should "send commitment TX to the broker after getting his refund TX signed" in {
    gateway.send(actor, fromCounterpart(RefundTxSignatureResponse("id", handshake.refundSignature)))
    shouldForwardToBroker(EnterExchange("id", handshake.commitmentTransaction.bitcoinSerialize()))
  }

  it should "sign counterpart refund after having our refund signed" in {
    shouldSignCounterpartRefund()
  }

  val publishedTransactions = Set(
    handshake.commitmentTransaction.getHash,
    handshake.counterpartCommitmentTransaction.getHash
  )

  it should "wait until the broker publishes commitments" in {
    listener.expectNoMsg(100 millis)
    gateway.send(actor, fromBroker(CommitmentNotification(
      "id",
      handshake.commitmentTransaction.getHash,
      handshake.counterpartCommitmentTransaction.getHash
    )))
    val confirmations = protocolConstants.commitmentConfirmations
    blockchain.expectMsgAllOf(
      NotifyWhenConfirmed(handshake.commitmentTransaction.getHash, confirmations),
      NotifyWhenConfirmed(handshake.counterpartCommitmentTransaction.getHash, confirmations)
    )
  }

  it should "wait until commitments are confirmed" in {
    listener.expectNoMsg(100 millis)
    publishedTransactions.foreach(tx => blockchain.send(actor, TransactionConfirmed(tx, 1)))
    listener.expectMsg(HandshakeResult(Success(handshake.refundSignature)))
  }

  it should "finally terminate himself" in {
    listener.expectTerminated(actor)
  }
}
