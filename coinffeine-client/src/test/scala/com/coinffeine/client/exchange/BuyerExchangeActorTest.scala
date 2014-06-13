package com.coinffeine.client.exchange

import scala.concurrent.duration._

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeActor.{ExchangeSuccess, StartExchange}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.{Market, OrderSet}
import com.coinffeine.common.protocol.messages.exchange.{PaymentProof, StepSignatures}

class BuyerExchangeActorTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val listener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
  val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)
  val exchange = new MockExchange(exchangeInfo) with BuyerUser[Euro.type]
  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart
  val actor = system.actorOf(Props[BuyerExchangeActor[Euro.type]], "buyer-exchange-actor")
  val dummySig = TransactionSignature.dummy
  listener.watch(actor)

  "The buyer exchange actor" should "subscribe to the relevant messages when initialized" in {
    gateway.expectNoMsg()
    actor ! StartExchange(exchange, protocolConstants, gateway.ref, Set(listener.ref))
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val relevantOfferAccepted = StepSignatures("id", dummySig, dummySig)
    val irrelevantOfferAccepted = StepSignatures("another-id", dummySig, dummySig)
    val anotherPeer = PeerConnection("some-random-peer")
    filter(fromCounterpart(relevantOfferAccepted)) should be (true)
    filter(ReceiveMessage(relevantOfferAccepted, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantOfferAccepted)) should be (false)
    val randomMessage = OrderSet.empty(Market(Euro))
    filter(ReceiveMessage(randomMessage, exchangeInfo.counterpart)) should be (false)
  }

  it should "respond to step signature messages by sending a payment until all " +
    "steps have are done" in {
      for (i <- 1 to exchangeInfo.steps) {
        actor ! fromCounterpart(StepSignatures(exchangeInfo.id, dummySig, dummySig))
        val paymentMsg = PaymentProof(exchangeInfo.id, "paymentId")
        shouldForward(paymentMsg) to counterpart
        gateway.expectNoMsg(100 milliseconds)
      }
    }

  it should "send a notification to the listeners once the exchange has finished" in {
    actor ! fromCounterpart(StepSignatures(exchangeInfo.id, dummySig, dummySig))
    listener.expectMsg(ExchangeSuccess)
  }
}
