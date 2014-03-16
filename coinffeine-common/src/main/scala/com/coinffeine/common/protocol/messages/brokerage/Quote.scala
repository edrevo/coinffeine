package com.coinffeine.common.protocol.messages.brokerage

import java.util.Currency

import com.coinffeine.common.currency.FiatAmount
import com.coinffeine.common.protocol.Spread
import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

case class Quote(
    currency: Currency,
    spread: Spread = (None, None),
    lastPrice: Option[FiatAmount] = None) {
  requireCurrency(spread._1)
  requireCurrency(spread._2)
  requireCurrency(lastPrice)

  override def toString = "Quote(spread = (%s, %s), last = %s)".format(
    spread._1.getOrElse("--"),
    spread._2.getOrElse("--"),
    lastPrice.getOrElse("--")
  )

  private def requireCurrency(amount: Option[FiatAmount]): Unit = require(
    amount.filter(_.currency != currency).isEmpty,
    s"Inconsistent price ${amount.get}, $currency was expected"
  )
}

object Quote {

  def empty(currency: Currency): Quote = Quote(currency)

  /** Utility constructor for the case of having all prices defined */
  def apply(spread: (FiatAmount, FiatAmount), lastPrice: FiatAmount): Quote = Quote(
    currency = lastPrice.currency,
    spread = Some(spread._1) -> Some(spread._2),
    lastPrice = Some(lastPrice)
  )

  implicit val Write = new MessageSend[Quote] {
    override def sendAsProto(msg: Quote, session: PeerSession) = ???
  }
}
