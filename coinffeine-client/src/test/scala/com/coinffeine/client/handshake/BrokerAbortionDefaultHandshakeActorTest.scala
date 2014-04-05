package com.coinffeine.client.handshake

import scala.concurrent.duration._

import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.messages.handshake.ExchangeAborted

class BrokerAbortionDefaultHandshakeActorTest
  extends DefaultHandshakeActorTest("broker-aborts") {

  override def protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 10 seconds,
    refundSignatureAbortTimeout = 10 seconds
  )

  "Handshakes aborted by the broker" should "make the handshake to fail" in {
    givenActorIsInitialized()
    gateway.send(actor, fromBroker(ExchangeAborted(handshake.exchangeInfo.id, "test abortion")))
    val result = listener.expectMsgClass(classOf[HandshakeResult]).refundSig
    result should be ('failure)
    result.toString should include ("test abortion")
  }

  it should "terminate the handshake" in {
    listener.expectTerminated(actor)
  }
}
