package com.coinffeine.market

import com.coinffeine.common.{FiatAmount, BitcoinAmount}
import com.coinffeine.common.protocol.messages.brokerage.OrderMatch

case class Cross(amount: BitcoinAmount, price: FiatAmount, buyer: ClientId, seller: ClientId) {
  def toOrderMatch(exchangeId: String) = OrderMatch(
    exchangeId, amount, price, buyer, seller)
}
