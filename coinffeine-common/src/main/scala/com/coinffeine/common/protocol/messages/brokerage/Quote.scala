package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{FiatCurrency, CurrencyAmount}
import com.coinffeine.common.protocol.Spread
import com.coinffeine.common.protocol.messages.PublicMessage

case class Quote[+C <: FiatCurrency](
    currency: C,
    spread: Spread[C] = (None, None),
    lastPrice: Option[CurrencyAmount[C]] = None) extends PublicMessage {
  override def toString = "Quote(spread = (%s, %s), last = %s)".format(
    spread._1.getOrElse("--"),
    spread._2.getOrElse("--"),
    lastPrice.getOrElse("--")
  )
}

object Quote {
  def empty[C <: FiatCurrency](currency: C): Quote[C] = Quote(currency)

  /** Utility constructor for the case of having all prices defined */
  def apply[C <: FiatCurrency](
      spread: (CurrencyAmount[C], CurrencyAmount[C]), lastPrice: CurrencyAmount[C]): Quote[C] =
    Quote(
      currency = lastPrice.currency,
      spread = Some(spread._1) -> Some(spread._2),
      lastPrice = Some(lastPrice)
    )
}
