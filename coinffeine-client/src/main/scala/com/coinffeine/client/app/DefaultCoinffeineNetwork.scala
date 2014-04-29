package com.coinffeine.client.app

import java.util.Currency
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout

import com.coinffeine.client.api.{CoinffeineNetwork, Exchange}
import com.coinffeine.client.api.CoinffeineNetwork.{Connected, Disconnected, ExchangeListener}
import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.protocol.messages.brokerage.{Order, Quote, QuoteRequest}

class DefaultCoinffeineNetwork(peer: ActorRef) extends CoinffeineNetwork {
  implicit private val timeout = Timeout(3.seconds)

  override def status = Disconnected

  override def connect(): Future[Connected] = ???
  override def disconnect(): Future[Disconnected.type] = ???

  override def currentQuote(paymentProcessorId: String, currency: Currency): Future[Quote] =
    (peer ? QuoteRequest(currency)).mapTo[Quote]

  override def exchanges: Set[Exchange] = Set.empty

  override def onExchangeChanged(listener: ExchangeListener): Unit = ???

  override def orders: Set[Order] = Set.empty

  override def cancelOrder(order: Order): Unit = ???

  override def submitBuyOrder(btcAmount: BtcAmount, paymentProcessorId: String, fiatAmount: FiatAmount) = ???

  override def submitSellOrder(btcAmount: BtcAmount, paymentProcessorId: String, fiatAmount: FiatAmount) = ???
}
