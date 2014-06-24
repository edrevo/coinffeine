package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{BitcoinAmount, FiatAmount, PeerConnection}
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.exchange.ExchangeId

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    exchangeId: ExchangeId,
    amount: BitcoinAmount,
    price: FiatAmount,
    buyer: PeerConnection,
    seller: PeerConnection
) extends PublicMessage {
  def participants: Set[PeerConnection] = Set(buyer, seller)
}
