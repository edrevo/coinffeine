package com.coinffeine.market

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.protocol.messages.brokerage.OrderMatch

case class Cross(amount: BtcAmount, price: Price, buyer: ClientId, seller: ClientId) {
  def toOrderMatch(exchangeId: String) = OrderMatch(exchangeId, amount, price, buyer, seller)
}
