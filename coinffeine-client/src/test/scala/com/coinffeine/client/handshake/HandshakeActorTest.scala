package com.coinffeine.client.handshake

import akka.actor.Props
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.SellerPerspective
import com.coinffeine.client.handshake.HandshakeActor.StartHandshake
import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.exchange.MockHandshake
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.ReceiveMessage
import com.coinffeine.common.protocol.messages.handshake.{PeerHandshake, PeerHandshakeAccepted}

/** Test fixture for testing the handshake actor interaction, one derived class per scenario. */
abstract class HandshakeActorTest(systemName: String)
  extends CoinffeineClientTest(systemName) with SellerPerspective with BitcoinjTest with MockitoSugar {

  def protocolConstants: ProtocolConstants

  lazy val handshake = new MockHandshake(exchange, userRole)
  val listener = TestProbe()
  val blockchain = TestProbe()
  val actor = system.actorOf(Props[HandshakeActor[Euro.type]], "handshake-actor")
  listener.watch(actor)

  def givenActorIsInitialized(): Unit = {
    actor ! StartHandshake(
      exchange, userRole, handshake, protocolConstants, gateway.ref, blockchain.ref, Set(listener.ref)
    )
  }

  def shouldForwardRefundSignatureRequest(): Unit = {
    val refundSignatureRequest = PeerHandshake(
      exchange.id, handshake.myUnsignedRefund, user.paymentProcessorAccount)
    shouldForward (refundSignatureRequest) to counterpartConnection
  }

  def shouldSignCounterpartRefund(): Unit = {
    val request = PeerHandshake(exchange.id, ImmutableTransaction(handshake.counterpartRefund),
      user.paymentProcessorAccount)
    gateway.send(actor, ReceiveMessage(request, counterpartConnection))
    val refundSignatureRequest =
      PeerHandshakeAccepted(exchange.id, handshake.counterpartRefundSignature)
    shouldForward (refundSignatureRequest) to counterpartConnection
  }

  override protected def resetBlockchainBetweenTests = false
}
