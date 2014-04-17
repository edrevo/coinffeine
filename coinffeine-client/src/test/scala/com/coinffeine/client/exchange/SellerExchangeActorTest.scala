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
import com.coinffeine.client.exchange.ExchangeActor.{StartExchange, ExchangeSuccess}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.paymentprocessor.Payment
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, Subscribe}
import com.coinffeine.common.protocol.messages.brokerage.CancelOrder
import com.coinffeine.common.protocol.messages.exchange._

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
    Props(new SellerExchangeActor(exchange, protocolConstants)),
    "seller-exchange-actor")
  listener.watch(actor)

  actor ! StartExchange(exchangeInfo, gateway.ref, Set(listener.ref))

  "The seller exchange actor" should "subscribe to the relevant messages" in {
    val Subscribe(filter) = gateway.expectMsgClass(classOf[Subscribe])
    val anotherPeer = PeerConnection("some-random-peer")
    val relevantPayment = PaymentProof("id", null)
    val irrelevantPayment = PaymentProof("another-id", null)
    filter(fromCounterpart(relevantPayment)) should be (true)
    filter(fromCounterpart(irrelevantPayment)) should be (false)
    val randomMessage = CancelOrder(CurrencyCode.EUR.currency)
    filter(ReceiveMessage(randomMessage, exchangeInfo.counterpart)) should be (false)
  }

  it should "send the first step signature as soon as the exchange starts" in {
    val offerSignature = exchange.sign(exchange.getOffer(1), exchangeInfo.userKey)
    shouldForward(StepSignature(exchangeInfo.id, offerSignature)) to counterpart
  }

  it should "not send the second step signature until payment proof has been provided" in {
    gateway.expectNoMsg(100 milliseconds)
  }

  it should "send the second step signature once payment proof has been provided" in {
    actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
    val offerSignature = exchange.sign(exchange.getOffer(2), exchangeInfo.userKey)
    shouldForward(StepSignature(exchangeInfo.id, offerSignature)) to counterpart
  }

  it should "send step signatures as new payment proofs are provided" in {
    actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
    for (i <- 3 to exchangeInfo.steps) {
      actor ! fromCounterpart(PaymentProof(exchangeInfo.id, "PROOF!"))
      val offerSignature = exchange.sign(exchange.getOffer(i), exchangeInfo.userKey)
      shouldForward(StepSignature(exchangeInfo.id, offerSignature)) to counterpart
    }
  }

  it should "send the final signature" in {
    val offerSignature = exchange.sign(exchange.finalOffer, exchangeInfo.userKey)
    shouldForward(StepSignature(exchangeInfo.id, offerSignature)) to counterpart
  }

  it should "send a notification to the listeners once the exchange has finished" in {
    listener.expectMsg(ExchangeSuccess)
  }
}
