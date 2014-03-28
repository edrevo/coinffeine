package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.testkit.TestProbe
import akka.actor.Props
import com.google.bitcoin.core.Transaction
import com.google.bitcoin.crypto.TransactionSignature
import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeActor.ExchangeSuccess
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.paymentprocessor.Payment
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange.{PaymentProof, OfferTransaction, OfferSignature}
import com.coinffeine.common.protocol.messages.brokerage.OrderCancellation
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.protocol.serialization.FakeTransactionSerialization

class BuyerExchangeActorTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val listener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
  val transactionSerialization = new FakeTransactionSerialization(Seq.empty, Seq.empty)
  val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)
  val exchange = new Exchange {
    private val offers = (1 to exchangeInfo.steps).map(idx => {
      val tx = new Transaction(exchangeInfo.network)
      tx.setLockTime(idx.toLong)
      tx
    })
    offers.map(_.hashCode()).distinct should have (size (exchangeInfo.steps))
    override def mustPay(step: Int): Boolean = step + 1 < exchangeInfo.steps
    override def validateSignature(step: Int, signature: TransactionSignature): Boolean = true
    override def getOffer(step: Int): Transaction = offers(step - 1)
    override def pay(step: Int): Future[Payment] = Future.successful(Payment(
      "paymentId", "sender", "receiver", 0.1 EUR, DateTime.now(), "description"))
  }
  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart
  val actor = system.actorOf(
    Props(new BuyerExchangeActor(
      exchangeInfo,
      exchange,
      gateway.ref,
      transactionSerialization,
      protocolConstants,
      Seq(listener.ref))),
    "buyer-exchange-actor")
    listener.watch(actor)

  "The buyer exchange actor" should "subscribe to the relevant messages" in {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val relevantOfferAccepted = OfferSignature("id", null)
    val irrelevantOfferAccepted = OfferSignature("another-id", null)
    val anotherPeer = PeerConnection("some-random-peer")
    filter(fromCounterpart(relevantOfferAccepted)) should be (true)
    filter(ReceiveMessage(relevantOfferAccepted, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantOfferAccepted)) should be (false)
    val randomMessage = OrderCancellation(CurrencyCode.EUR.currency)
    filter(ReceiveMessage(randomMessage, exchangeInfo.counterpart)) should be (false)
  }

  it should "send the first offer as soon as it gets created" in {
    shouldForward (OfferTransaction(exchangeInfo.id, exchange.getOffer(1))) to counterpart
    gateway.expectNoMsg(100 milliseconds)
  }

  it should "respond to offer accepted messages by sending a payment and a new offer until all " +
    "steps have are done" in {
      for (i <- 1 to exchangeInfo.steps) {
        actor ! fromCounterpart(OfferSignature(exchangeInfo.id, TransactionSignature.dummy))
        if (exchange.mustPay(i)) {
          val paymentMsg = PaymentProof(exchangeInfo.id, "paymentId")
          val newOfferMsg = OfferTransaction(exchangeInfo.id, exchange.getOffer(i + 1))
          shouldForwardAll message(paymentMsg) message(newOfferMsg) to counterpart
        }
        gateway.expectNoMsg(100 milliseconds)
      }
    }

  it should "send a notification to the listeners once the exchange has finished" in {
    listener.expectMsg(ExchangeSuccess)
  }
}
