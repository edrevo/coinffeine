package com.coinffeine

import com.coinffeine.common.{BitcoinAmount, CurrencyAmount, FiatCurrency, PeerConnection}
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid}

package object market {

  /** PeerConnection should be replaced in the near term with a secure client id. */
  type ClientId = PeerConnection

  type Price[C <: FiatCurrency] = CurrencyAmount[C]
  type OrderQueue = Seq[(BitcoinAmount, ClientId)]
  type BidMap[C <: FiatCurrency] = OrderMap[Bid.type, C]
  type AskMap[C <: FiatCurrency] = OrderMap[Ask.type, C]
}
