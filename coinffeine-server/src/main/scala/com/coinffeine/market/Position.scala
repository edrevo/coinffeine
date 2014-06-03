package com.coinffeine.market

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.messages.brokerage.Order

/** Bidding or asking position taken by a requester */
case class Position(requester: PeerConnection, order: Order)
