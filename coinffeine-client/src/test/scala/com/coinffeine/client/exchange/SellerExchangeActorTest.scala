package com.coinffeine.client.exchange

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.testkit.TestProbe
import akka.actor.Props
import com.google.bitcoin.core.{ECKey, Transaction}
import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeActor.ExchangeSuccess
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.paymentprocessor.Payment
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.exchange._
import com.coinffeine.common.protocol.messages.brokerage.CancelOrder
import com.coinffeine.common.protocol.messages.exchange.{NewOffer, PaymentProof}

class SellerExchangeActorTest extends CoinffeineClientTest("sellerExchange") with MockitoSugar {
  val listener = TestProbe()
  val exchangeInfo = sampleExchangeInfo
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
    override def validateSignature(step: Int, signature: TransactionSignature): Boolean = true
    override def getOffer(step: Int): Transaction = offers(step - 1)
    override def pay(step: Int): Future[Payment] = ???
    override def validatePayment(step: Int, paymentId: String): Boolean = true
    override def sign(offer: Transaction, key: ECKey): TransactionSignature = TransactionSignature.dummy()
    override def validateFinalSignature(signature: TransactionSignature): Boolean = ???

    override val finalOffer: Transaction = {
      val tx = new Transaction(exchangeInfo.network)
      tx.setLockTime(1500L)
      tx
    }
  }
  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart
  val actor = system.actorOf(
    Props(new SellerExchangeActor(
      exchangeInfo,
      exchange,
      gateway.ref,
      protocolConstants,
      Seq(listener.ref))),
    "seller-exchange-actor")
  listener.watch(actor)

  "The seller exchange actor" should "subscribe to the relevant messages" in {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val relevantOffer = NewOffer("id", null)
    val irrelevantOffer = NewOffer("another-id", null)
    val anotherPeer = PeerConnection("some-random-peer")
    val relevantPayment = PaymentProof("id", null)
    val irrelevantPayment = PaymentProof("another-id", null)
    filter(fromCounterpart(relevantOffer)) should be (true)
    filter(fromCounterpart(relevantPayment)) should be (true)
    filter(ReceiveMessage(relevantOffer, anotherPeer)) should be (false)
    filter(fromCounterpart(irrelevantOffer)) should be (false)
    filter(fromCounterpart(irrelevantPayment)) should be (false)
    val randomMessage = CancelOrder(CurrencyCode.EUR.currency)
    filter(ReceiveMessage(randomMessage, exchangeInfo.counterpart)) should be (false)
  }

  it should "ignore unexpected offers" in {
    actor ! fromCounterpart(NewOffer(exchangeInfo.id, exchange.getOffer(2)))
    gateway.expectNoMsg(100 milliseconds)
  }

  it should "sign the first offer as soon as it receives it" in {
    actor ! fromCounterpart(NewOffer(exchangeInfo.id, exchange.getOffer(1)))
    val offerSignature = exchange.sign(exchange.getOffer(1), exchangeInfo.userKey)
    shouldForward(OfferAccepted(exchangeInfo.id, offerSignature)) to counterpart
  }

  it should "not sign the second offer until payment proof has been provided" in {
    actor ! fromCounterpart(NewOffer(exchangeInfo.id, exchange.getOffer(2)))
    gateway.expectNoMsg(100 milliseconds)
  }

  it should "sign the second offer once payment proof has been provided" in {
    actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
    val offerSignature = exchange.sign(exchange.getOffer(2), exchangeInfo.userKey)
    shouldForward(OfferAccepted(exchangeInfo.id, offerSignature)) to counterpart
  }

  it should "sign offers once payment proof has been provided" in {
    actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
    for (i <- 3 to exchangeInfo.steps) {
      actor ! fromCounterpart(NewOffer(exchangeInfo.id, exchange.getOffer(i)))
      actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
      val offerSignature = exchange.sign(exchange.getOffer(i), exchangeInfo.userKey)
      shouldForward(OfferAccepted(exchangeInfo.id, offerSignature)) to counterpart
    }
  }

  it should "sign the final offer" in {
    actor ! fromCounterpart(NewOffer(exchangeInfo.id, exchange.finalOffer))
    val offerSignature = exchange.sign(exchange.finalOffer, exchangeInfo.userKey)
    shouldForward(OfferAccepted(exchangeInfo.id, offerSignature)) to counterpart
  }

  it should "send a notification to the listeners once the exchange has finished" in {
    listener.expectMsg(ExchangeSuccess)
  }
}
