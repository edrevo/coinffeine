package com.coinffeine.client.handshake

import akka.actor.Props
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.handshake.HandshakeActor.StartHandshake
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway.ReceiveMessage
import com.coinffeine.common.protocol.messages.handshake.{RefundTxSignatureRequest, RefundTxSignatureResponse}

/** Test fixture for testing the handshake actor interaction, one derived class per scenario. */
abstract class DefaultHandshakeActorTest(systemName: String)
  extends CoinffeineClientTest(systemName) with MockitoSugar {

  def protocolConstants: ProtocolConstants

  val handshake = new MockHandshake(sampleExchangeInfo)
  override val counterpart = handshake.exchangeInfo.counterpart
  override val broker = handshake.exchangeInfo.broker
  val listener = TestProbe()
  val blockchain = TestProbe()
  val actor = system.actorOf(Props[DefaultHandshakeActor[Euro.type]], "handshake-actor")
  listener.watch(actor)

  def givenActorIsInitialized(): Unit =
    actor ! StartHandshake(handshake, protocolConstants, gateway.ref, blockchain.ref, Set(listener.ref))

  def shouldForwardRefundSignatureRequest(): Unit = {
    val refundSignatureRequest = RefundTxSignatureRequest("id", handshake.refundTransaction)
    shouldForward (refundSignatureRequest) to counterpart
  }

  def shouldSignCounterpartRefund(): Unit = {
    val request = RefundTxSignatureRequest("id", handshake.counterpartRefund)
    gateway.send(actor, ReceiveMessage(request, handshake.exchangeInfo.counterpart))
    val refundSignatureRequest = RefundTxSignatureResponse("id", handshake.counterpartRefundSignature)
    shouldForward (refundSignatureRequest) to counterpart
  }
}
