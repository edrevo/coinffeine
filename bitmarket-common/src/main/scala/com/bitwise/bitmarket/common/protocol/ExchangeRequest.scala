package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.BtcAmount

case class ExchangeRequest(
    exchangeId: String,
    fromId: PeerId,
    fromConnection: PeerConnection,
    amount: BtcAmount
)
