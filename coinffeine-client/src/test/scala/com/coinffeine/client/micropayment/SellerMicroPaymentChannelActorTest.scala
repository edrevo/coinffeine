package com.coinffeine.client.micropayment

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props
import akka.testkit.TestProbe
import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.SellerPerspective
import com.coinffeine.client.exchange.{MockProtoMicroPaymentChannel, PaymentDescription}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, StartMicroPaymentChannel}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.exchange.Exchange
import com.coinffeine.common.paymentprocessor.Payment
import com.coinffeine.common.paymentprocessor.PaymentProcessor.{FindPayment, PaymentFound}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.{Market, OrderSet}
import com.coinffeine.common.protocol.messages.exchange._

class SellerMicroPaymentChannelActorTest extends CoinffeineClientTest("sellerExchange")
  with SellerPerspective with MockitoSugar {

  val listener = TestProbe()
  val paymentProcessor = TestProbe()
  val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)
  val channel = new MockProtoMicroPaymentChannel(exchange)
  val actor = system.actorOf(Props[SellerMicroPaymentChannelActor[Euro.type]], "seller-exchange-actor")
  listener.watch(actor)

  actor ! StartMicroPaymentChannel(
    exchange, userRole, channel, protocolConstants, paymentProcessor.ref, gateway.ref,
    Set(listener.ref)
  )

  "The seller exchange actor" should "subscribe to the relevant messages" in {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val anotherPeer = PeerConnection("some-random-peer")
    val relevantPayment = PaymentProof(exchange.id, null)
    val irrelevantPayment = PaymentProof(Exchange.Id("another-id"), null)
    filter(fromCounterpart(relevantPayment)) should be (true)
    filter(ReceiveMessage(relevantPayment, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantPayment)) should be (false)
    val randomMessage = OrderSet.empty(Market(Euro))
    filter(ReceiveMessage(randomMessage, counterpart.connection)) should be (false)
  }

  it should "send the first step signature as soon as the exchange starts" in {
    val offerSignature = channel.signStep(1)
    shouldForward(StepSignatures(exchange.id, 1, offerSignature.toTuple)) to counterpart.connection
  }

  it should "not send the second step signature until payment proof has been provided" in {
    gateway.expectNoMsg(100 milliseconds)
  }

  it should "send the second step signature once payment proof has been provided" in {
    actor ! fromCounterpart(PaymentProof(exchange.id, "PROOF!"))
    expectPayment(1)
    val signatures = StepSignatures(exchange.id, 2, channel.signStep(2).toTuple)
    shouldForward(signatures) to counterpart.connection
  }

  it should "send step signatures as new payment proofs are provided" in {
    actor ! fromCounterpart(PaymentProof(exchange.id, "PROOF!"))
    expectPayment(2)
    for (i <- 3 to exchange.parameters.breakdown.intermediateSteps) {
      actor ! fromCounterpart(PaymentProof(exchange.id, "PROOF!"))
      expectPayment(i)
      val signatures = StepSignatures(exchange.id, i, channel.signStep(i).toTuple)
      shouldForward(signatures) to counterpart.connection
    }
  }

  it should "send the final signature" in {
    val signatures = StepSignatures(
      exchange.id, exchange.parameters.breakdown.totalSteps, channel.finalSignatures.toTuple)
    shouldForward(signatures) to counterpart.connection
  }

  it should "send a notification to the listeners once the exchange has finished" in {
    listener.expectMsg(ExchangeSuccess)
  }

  private def expectPayment(step: Int): Unit = {
    val FindPayment(paymentId) = paymentProcessor.expectMsgClass(classOf[FindPayment])
    paymentProcessor.reply(PaymentFound(Payment(
      id = paymentId,
      senderId = exchange.buyer.paymentProcessorAccount,
      receiverId = exchange.seller.paymentProcessorAccount,
      description = PaymentDescription(exchange.id, step),
      amount = exchange.amounts.stepFiatAmount,
      date = DateTime.now()
    )))
  }
}
