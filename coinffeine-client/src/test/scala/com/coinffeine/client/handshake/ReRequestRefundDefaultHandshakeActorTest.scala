package com.coinffeine.client.handshake

import scala.concurrent.duration._

import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway.Subscribe

class ReRequestRefundDefaultHandshakeActorTest extends DefaultHandshakeActorTest("happy-path") {

  override def protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 500 millis,
    refundSignatureAbortTimeout = 1 minute
  )

  "The handshake actor" should "request refund transaction signature after a timeout" in {
    givenActorIsInitialized()
    gateway.expectMsgClass(classOf[Subscribe])
    shouldForwardRefundSignatureRequest()
    shouldForwardRefundSignatureRequest()
  }

  it should "request it again after signing counterpart refund" in {
    shouldSignCounterpartRefund()
    shouldForwardRefundSignatureRequest()
  }
}
