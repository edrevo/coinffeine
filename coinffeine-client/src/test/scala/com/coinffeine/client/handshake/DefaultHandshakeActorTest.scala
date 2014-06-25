package com.coinffeine.client.handshake

import akka.actor.Props
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.handshake.HandshakeActor.StartHandshake
import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.bitcoin.{ImmutableTransaction, KeyPair}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.ReceiveMessage
import com.coinffeine.common.protocol.messages.handshake.{RefundTxSignatureRequest, RefundTxSignatureResponse}

/** Test fixture for testing the handshake actor interaction, one derived class per scenario. */
abstract class DefaultHandshakeActorTest(systemName: String)
  extends CoinffeineClientTest(systemName) with BitcoinjTest with MockitoSugar {

  def protocolConstants: ProtocolConstants

  lazy val handshake = new MockHandshake(sampleExchangeInfo.exchange, sampleExchangeInfo.role,
    amount => createWallet(new KeyPair, amount), network)
  val listener = TestProbe()
  val blockchain = TestProbe()
  val actor = system.actorOf(Props[DefaultHandshakeActor[Euro.type]], "handshake-actor")
  listener.watch(actor)

  def givenActorIsInitialized(): Unit = {
    actor ! StartHandshake(handshake.exchange, handshake.role, handshake, protocolConstants,
      gateway.ref, blockchain.ref, Set(listener.ref))
  }

  def shouldForwardRefundSignatureRequest(): Unit = {
    val refundSignatureRequest = RefundTxSignatureRequest(exchangeId, handshake.myUnsignedRefund)
    shouldForward (refundSignatureRequest) to counterpart
  }

  def shouldSignCounterpartRefund(): Unit = {
    val request =
      RefundTxSignatureRequest(exchangeId, ImmutableTransaction(handshake.counterpartRefund))
    gateway.send(actor, ReceiveMessage(request, sampleExchangeInfo.counterpart.connection))
    val refundSignatureRequest =
      RefundTxSignatureResponse(exchangeId, handshake.counterpartRefundSignature)
    shouldForward (refundSignatureRequest) to counterpart
  }

  override def counterpart = sampleExchangeInfo.counterpart.connection
  override def broker = handshake.exchange.broker.connection

  override protected def resetBlockchainBetweenTests = false
}
