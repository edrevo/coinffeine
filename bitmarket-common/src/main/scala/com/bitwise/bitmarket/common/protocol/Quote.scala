package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.currency.FiatAmount

case class Quote(spread: Spread = (None, None), lastPrice: Option[FiatAmount] = None) {
  require(
    Set(spread._1, spread._2, lastPrice).flatten.map(_.currency).size <= 1,
    "All prices should use the same currency"
  )

  override def toString = "Quote(spread = (%s, %s), last = %s)".format(
    spread._1.getOrElse("--"),
    spread._2.getOrElse("--"),
    lastPrice.getOrElse("--")
  )
}

object Quote {

  /** Utility constructor for the case of having all prices defined */
  def apply(spread: (FiatAmount, FiatAmount), lastPrice: FiatAmount): Quote =
    Quote(spread = Some(spread._1) -> Some(spread._2), lastPrice = Some(lastPrice))
}
