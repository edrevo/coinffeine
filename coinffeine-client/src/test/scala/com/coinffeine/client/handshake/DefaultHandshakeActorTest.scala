package com.coinffeine.client.handshake

import akka.actor.Props
import akka.testkit.TestProbe
import com.coinffeine.common.exchange.MockHandshake
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.SellerPerspective
import com.coinffeine.client.handshake.HandshakeActor.StartHandshake
import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.ReceiveMessage
import com.coinffeine.common.protocol.messages.handshake.{RefundTxSignatureRequest, RefundTxSignatureResponse}

/** Test fixture for testing the handshake actor interaction, one derived class per scenario. */
abstract class DefaultHandshakeActorTest(systemName: String)
  extends CoinffeineClientTest(systemName) with SellerPerspective with BitcoinjTest with MockitoSugar {

  def protocolConstants: ProtocolConstants

  lazy val handshake = new MockHandshake(exchange, userRole)
  val listener = TestProbe()
  val blockchain = TestProbe()
  val actor = system.actorOf(Props[DefaultHandshakeActor[Euro.type]], "handshake-actor")
  listener.watch(actor)

  def givenActorIsInitialized(): Unit = {
    actor ! StartHandshake(
      exchange, userRole, handshake, protocolConstants, gateway.ref, blockchain.ref, Set(listener.ref)
    )
  }

  def shouldForwardRefundSignatureRequest(): Unit = {
    val refundSignatureRequest = RefundTxSignatureRequest(exchange.id, handshake.myUnsignedRefund)
    shouldForward (refundSignatureRequest) to counterpart.connection
  }

  def shouldSignCounterpartRefund(): Unit = {
    val request =
      RefundTxSignatureRequest(exchange.id, ImmutableTransaction(handshake.counterpartRefund))
    gateway.send(actor, ReceiveMessage(request, userRole.her(exchange).connection))
    val refundSignatureRequest =
      RefundTxSignatureResponse(exchange.id, handshake.counterpartRefundSignature)
    shouldForward (refundSignatureRequest) to counterpart.connection
  }

  override protected def resetBlockchainBetweenTests = false
}
