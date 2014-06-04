package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.protocol.messages.PublicMessage

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    exchangeId: String,
    amount: BtcAmount,
    price: FiatAmount,
    buyer: PeerConnection,
    seller: PeerConnection
) extends PublicMessage {
  def participants: Set[PeerConnection] = Set(buyer, seller)
}
