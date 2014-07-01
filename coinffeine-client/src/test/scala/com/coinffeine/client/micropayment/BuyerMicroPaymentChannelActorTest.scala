package com.coinffeine.client.micropayment

import scala.concurrent.duration._

import akka.actor.Props
import akka.testkit.TestProbe
import org.joda.time.DateTime

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.BuyerPerspective
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, GetLastOffer, LastOffer, StartMicroPaymentChannel}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.exchange.{Exchange, MockExchangeProtocol}
import com.coinffeine.common.exchange.MicroPaymentChannel.Signatures
import com.coinffeine.common.paymentprocessor.{Payment, PaymentProcessor}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.{Market, OrderSet}
import com.coinffeine.common.protocol.messages.exchange.{PaymentProof, StepSignatures}

class BuyerMicroPaymentChannelActorTest
  extends CoinffeineClientTest("buyerExchange") with BuyerPerspective {

  val listener = TestProbe()
  val paymentProcessor = TestProbe()
  val protocolConstants = ProtocolConstants()
  val exchangeProtocol = new MockExchangeProtocol
  val actor = system.actorOf(
    Props(new BuyerMicroPaymentChannelActor(exchangeProtocol)),
    "buyer-exchange-actor"
  )
  val signatures = Signatures(TransactionSignature.dummy, TransactionSignature.dummy)
  val expectedLastOffer = {
    val initialChannel = exchangeProtocol
      .createMicroPaymentChannel(exchange, userRole, MockExchangeProtocol.DummyDeposits)
    val lastChannel = Seq.iterate(
      initialChannel, exchange.amounts.breakdown.totalSteps)(_.nextStep).last
    lastChannel.closingTransaction(signatures)
  }
  listener.watch(actor)

  "The buyer exchange actor" should "subscribe to the relevant messages when initialized" in {
    gateway.expectNoMsg()
    actor ! StartMicroPaymentChannel(exchange, userRole, MockExchangeProtocol.DummyDeposits,
      protocolConstants, paymentProcessor.ref, gateway.ref, Set(listener.ref))

    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val otherId = Exchange.Id("other-id")
    val relevantOfferAccepted = StepSignatures(exchange.id, 5, signatures)
    val irrelevantOfferAccepted = StepSignatures(otherId, 2, signatures)
    val anotherPeer = PeerConnection("some-random-peer")
    filter(fromCounterpart(relevantOfferAccepted)) should be (true)
    filter(ReceiveMessage(relevantOfferAccepted, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantOfferAccepted)) should be (false)
    val randomMessage = OrderSet.empty(Market(Euro))
    filter(ReceiveMessage(randomMessage, counterpartConnection)) should be (false)
  }

  it should "respond to step signature messages by sending a payment until all steps are done" in {
    for (i <- 1 to exchange.amounts.breakdown.intermediateSteps) {
      actor ! fromCounterpart(StepSignatures(exchange.id, i, signatures))
      paymentProcessor.expectMsgClass(classOf[PaymentProcessor.Pay[_]])
      paymentProcessor.reply(PaymentProcessor.Paid(
        Payment(s"payment$i", "sender", "receiver", 1.EUR, DateTime.now(), "description")
      ))
      shouldForward(PaymentProof(exchange.id, s"payment$i")) to counterpartConnection
      gateway.expectNoMsg(100 milliseconds)
    }
  }

  it should "send a notification to the listeners once the exchange has finished" in {
    actor ! fromCounterpart(
      StepSignatures(exchange.id, exchange.amounts.breakdown.totalSteps, signatures))
    listener.expectMsg(ExchangeSuccess(Some(expectedLastOffer)))
  }

  it should "reply with the final transaction when asked about the last signed offer" in {
    actor ! GetLastOffer
    expectMsg(LastOffer(Some(expectedLastOffer)))
  }
}
