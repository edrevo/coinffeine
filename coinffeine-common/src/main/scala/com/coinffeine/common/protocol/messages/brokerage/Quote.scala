package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.currency.FiatAmount
import com.coinffeine.common.protocol.Spread
import com.coinffeine.common.protocol.messages.MessageSend
import com.coinffeine.common.protorpc.PeerSession

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

  implicit val Write = new MessageSend[Quote] {
    override def sendAsProto(msg: Quote, session: PeerSession) = ???
  }
}
