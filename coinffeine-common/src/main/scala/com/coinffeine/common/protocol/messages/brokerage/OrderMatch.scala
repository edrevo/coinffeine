package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.{BitcoinAmount, FiatAmount, PeerConnection}

/** Represents a coincidence of desires of both a buyer and a seller */
case class OrderMatch(
    exchangeId: Exchange.Id,
    amount: BitcoinAmount,
    price: FiatAmount,
    participants: Both[PeerConnection]
) extends PublicMessage
