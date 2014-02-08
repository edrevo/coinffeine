package com.bitwise.bitmarket.client.handshake

import scala.concurrent.duration._
import scala.language.postfixOps

import com.bitwise.bitmarket.common.protocol.ExchangeAborted
import com.bitwise.bitmarket.client.ProtocolConstants
import com.bitwise.bitmarket.client.handshake.HandshakeActor._

class BrokerAbortionDefaultHandshakeActorTest
  extends DefaultHandshakeActorTest("broker-aborts") {

  override def protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 10 seconds,
    refundSignatureAbortTimeout = 10 seconds
  )

  "Handshakes aborted by the broker" should "make the handshake to fail" in {
    gateway.send(actor, fromBroker(ExchangeAborted(handshake.exchange.id, "test abortion")))
    val result = listener.expectMsgClass(classOf[HandshakeResult]).refundSig
    result should be ('failure)
    result.toString should include ("test abortion")
  }

  it should "terminate the handshake" in {
    listener.expectTerminated(actor)
  }
}
