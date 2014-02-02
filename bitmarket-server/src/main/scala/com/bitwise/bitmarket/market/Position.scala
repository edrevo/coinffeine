package com.bitwise.bitmarket.market

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.protocol.Order
import com.bitwise.bitmarket.common.currency.BtcAmount

/** Bidding or asking position taken by a requester */
case class Position(requester: PeerConnection, order: Order) {

  def reduceAmount(reduction: BtcAmount): Position =
    copy(order = order.copy(amount = order.amount - reduction))
}
