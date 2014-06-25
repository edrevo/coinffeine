package com.coinffeine.client.micropayment

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.{MockExchange, SellerUser}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, StartMicroPaymentChannel}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.{Market, OrderSet}
import com.coinffeine.common.protocol.messages.exchange._

class SellerMicroPaymentChannelActorTest extends CoinffeineClientTest("sellerExchange") with MockitoSugar {
  val listener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
  val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)
  val exchange = new MockExchange(exchangeInfo) with SellerUser[Euro.type]
  override val broker: PeerConnection = exchangeInfo.broker.connection
  override val counterpart: PeerConnection = exchangeInfo.counterpart.connection
  val actor = system.actorOf(Props[SellerMicroPaymentChannelActor[Euro.type]], "seller-exchange-actor")
  listener.watch(actor)

  actor ! StartMicroPaymentChannel(exchange, protocolConstants, gateway.ref, Set(listener.ref))

  "The seller exchange actor" should "subscribe to the relevant messages" in {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val anotherPeer = PeerConnection("some-random-peer")
    val relevantPayment = PaymentProof(exchangeId, null)
    val irrelevantPayment = PaymentProof(Exchange.Id("another-id"), null)
    filter(fromCounterpart(relevantPayment)) should be (true)
    filter(ReceiveMessage(relevantPayment, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantPayment)) should be (false)
    val randomMessage = OrderSet.empty(Market(Euro))
    filter(ReceiveMessage(randomMessage, exchangeInfo.counterpart.connection)) should be (false)
  }

  it should "send the first step signature as soon as the exchange starts" in {
    val offerSignature = exchange.signStep(1)
    shouldForward(StepSignatures(exchangeInfo.id, 1, offerSignature.toTuple)) to counterpart
  }

  it should "not send the second step signature until payment proof has been provided" in {
    gateway.expectNoMsg(100 milliseconds)
  }

  it should "send the second step signature once payment proof has been provided" in {
    actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
    shouldForward(StepSignatures(exchangeInfo.id, 2, exchange.signStep(2).toTuple)) to counterpart
  }

  it should "send step signatures as new payment proofs are provided" in {
    actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
    for (i <- 3 to exchangeInfo.parameters.breakdown.intermediateSteps) {
      actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
      shouldForward(StepSignatures(exchangeInfo.id, i, exchange.signStep(i).toTuple)) to counterpart
    }
  }

  it should "send the final signature" in {
    shouldForward(StepSignatures(
      exchangeInfo.id, exchangeInfo.parameters.breakdown.totalSteps, exchange.finalSignatures.toTuple)) to counterpart
  }

  it should "send a notification to the listeners once the exchange has finished" in {
    listener.expectMsg(ExchangeSuccess)
  }
}
