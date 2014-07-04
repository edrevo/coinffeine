package com.coinffeine.client.app

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorRef
import akka.pattern._

import com.coinffeine.client.api.MarketStats
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.protocol.messages.brokerage._

import scala.concurrent.Future

private[app] class DefaultMarketStats(override val peer: ActorRef)
  extends MarketStats with PeerActorWrapper {

  override def currentQuote[C <: FiatCurrency](market: Market[C]): Future[Quote[C]] =
    (peer ? QuoteRequest(market.currency)).mapTo[Quote[C]]

  override def openOrders[C <: FiatCurrency](market: Market[C]): Future[OrderSet[C]] =
    (peer ? OpenOrdersRequest(market.currency)).mapTo[OpenOrders[C]].map(_.orders)
}
