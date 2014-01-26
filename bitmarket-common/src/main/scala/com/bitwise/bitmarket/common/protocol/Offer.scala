package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.{FiatAmount, BtcAmount}

case class Offer(
    id: String,
    sequenceNumber: Int,
    fromId: PeerId,
    fromConnection: PeerConnection,
    amount: BtcAmount,
    bitcoinPrice: FiatAmount) {

  override def toString = s"offer with ID $id from $fromId of $amount ($bitcoinPrice per BTC)"
}
