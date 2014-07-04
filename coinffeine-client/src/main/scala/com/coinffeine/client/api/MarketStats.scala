package com.coinffeine.client.api

import scala.concurrent.Future

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.protocol.messages.brokerage.{Market, OrderSet, Quote}

/** Give access to current and historical prices and other market stats. */
trait MarketStats {

  /** Check current prices for a given market */
  def currentQuote[C <: FiatCurrency](market: Market[C]): Future[Quote[C]]

  /** Current open orders for a given market (anonymized order book) */
  def openOrders[C <: FiatCurrency](market: Market[C]): Future[OrderSet[C]]
}
