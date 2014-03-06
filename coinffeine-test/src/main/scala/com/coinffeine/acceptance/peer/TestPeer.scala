package com.coinffeine.acceptance.peer

import com.coinffeine.common.currency.CurrencyCode
import com.coinffeine.common.protocol.Order
import com.coinffeine.common.protocol.messages.brokerage.Quote

/** Testing façade for a Coinffeine peer. */
trait TestPeer {
  def askForQuote(currency: CurrencyCode): Unit
  def placeOrder(order: Order): Unit
  def lastQuote: Option[Quote]
  def shutdown(): Unit
}
