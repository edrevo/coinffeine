package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR

class OrderMatchTest extends UnitTest {

  "An order match" should "provide the match participants" in {
    val buyer = PeerConnection("buyer")
    val seller = PeerConnection("seller")
    val orderMatch = OrderMatch("some-exchange", BtcAmount(10), EUR(1000), buyer, seller)
    orderMatch.participants should be (Set(buyer, seller))
  }
}
