package com.coinffeine.client.exchange

import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{ActorRef, Actor, Props}
import akka.testkit.TestProbe
import com.google.bitcoin.core.{ECKey, Transaction}
import com.google.bitcoin.crypto.TransactionSignature
import org.joda.time.DateTime

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeActor.{ExchangeSuccess, StartExchange}
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.paymentprocessor.Payment
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ReceiveMessage, ForwardMessage}

class BuyerSellerCoordinationTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val buyerListener = TestProbe()
  val sellerListener = TestProbe()
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
    override def pay(step: Int): Future[Payment] = Future.successful(Payment(
      "paymentId", "sender", "receiver", 0.1 EUR, DateTime.now(), "description"))
    override def validatePayment(step: Int, paymentId: String): Boolean = true
    override def sign(offer: Transaction, key: ECKey): TransactionSignature = TransactionSignature.dummy()
    override def validateFinalSignature(signature: TransactionSignature): Boolean = true
    override val finalOffer: Transaction = {
      val tx = new Transaction(exchangeInfo.network)
      tx.setLockTime(1500L)
      tx
    }
  }

  class MessageForwarder(to: ActorRef) extends Actor {
    override val receive: Receive = {
      case ForwardMessage(msg, dest) => to ! ReceiveMessage(msg, dest)
    }
  }

  object MessageForwarder {
    def apply(name: String, to: ActorRef): ActorRef = system.actorOf(
      Props(new MessageForwarder(to)), name)
  }

  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart
  val buyer = system.actorOf(
    Props(new BuyerExchangeActor(exchange, protocolConstants)),
    "buyer-exchange-actor"
  )

  val seller = system.actorOf(
    Props(new SellerExchangeActor(exchange, protocolConstants)),
    "seller-exchange-actor"
  )

  "The buyer and seller actors" should "be able to perform an exchange" in {
    buyer ! StartExchange(
      exchangeInfo, MessageForwarder("fw-to-seller", seller), Set(buyerListener.ref))
    seller ! StartExchange(
      exchangeInfo, MessageForwarder("fw-to-buyer", buyer), Set(sellerListener.ref))
    buyerListener.expectMsg(ExchangeSuccess)
    sellerListener.expectMsg(ExchangeSuccess)
  }
}
