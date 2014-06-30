package com.coinffeine.client.micropayment

import akka.actor.{Actor, ActorRef, Props}
import akka.testkit.TestProbe
import org.scalatest.mock.MockitoSugar

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.handshake.MockExchangeProtocol
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{ExchangeSuccess, StartMicroPaymentChannel}
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange.{BuyerRole, SellerRole}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.gateway.MessageGateway.{ForwardMessage, ReceiveMessage}

class BuyerSellerCoordinationTest extends CoinffeineClientTest("buyerExchange") with MockitoSugar {
  val buyerListener = TestProbe()
  val sellerListener = TestProbe()
  val protocolConstants = ProtocolConstants()
  val paymentProcFactory = new MockPaymentProcessorFactory()
  val exchangeProtocol = new MockExchangeProtocol()

  class MessageForwarder(to: ActorRef) extends Actor {
    override val receive: Receive = {
      case ForwardMessage(msg, dest) => to ! ReceiveMessage(msg, dest)
    }
  }

  object MessageForwarder {
    def apply(name: String, to: ActorRef): ActorRef = system.actorOf(
      Props(new MessageForwarder(to)), name)
  }

  val buyerPaymentProc = system.actorOf(paymentProcFactory.newProcessor(
    exchange.buyer.paymentProcessorAccount, Seq(1000.EUR)))
  val buyer = system.actorOf(
    Props(new BuyerMicroPaymentChannelActor(exchangeProtocol)),
    "buyer-exchange-actor"
  )

  val sellerPaymentProc = system.actorOf(paymentProcFactory.newProcessor(
    exchange.seller.paymentProcessorAccount, Seq(0.EUR)))
  val seller = system.actorOf(
    Props(new SellerMicroPaymentChannelActor(exchangeProtocol)),
    "seller-exchange-actor"
  )

  "The buyer and seller actors" should "be able to perform an exchange" in {
    buyer ! StartMicroPaymentChannel(
      exchange, BuyerRole, MockExchangeProtocol.DummyDeposits, protocolConstants, buyerPaymentProc,
      MessageForwarder("fw-to-seller", seller), Set(buyerListener.ref))
    seller ! StartMicroPaymentChannel(
      exchange, SellerRole, MockExchangeProtocol.DummyDeposits, protocolConstants, sellerPaymentProc,
      MessageForwarder("fw-to-buyer", buyer), Set(sellerListener.ref))
    buyerListener.expectMsgClass(classOf[ExchangeSuccess])
    sellerListener.expectMsg(ExchangeSuccess(None))
  }
}
