package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{FiatAmount, BitcoinAmount, PeerConnection}
import com.coinffeine.common.protocol.messages.PublicMessage

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    exchangeId: String,
    amount: BitcoinAmount,
    price: FiatAmount,
    buyer: PeerConnection,
    seller: PeerConnection
) extends PublicMessage {
  def participants: Set[PeerConnection] = Set(buyer, seller)
}
