package com.coinffeine

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid}

package object market {

  /** PeerConnection should be replaced in the near term with a secure client id. */
  type ClientId = PeerConnection

  type Price = FiatAmount
  type OrderQueue = Seq[(BtcAmount, ClientId)]
  type BidMap = OrderMap[Bid.type]
  type AskMap = OrderMap[Ask.type]
}
