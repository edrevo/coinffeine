package com.coinffeine.client.exchange

import com.coinffeine.common.exchange.Exchange

/** Payment description formatter */
object PaymentDescription {
  def apply(exchangeId: Exchange.Id, step: Int): String = s"Payment for $exchangeId, step $step"
}
