package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.Currency.Implicits._

class OrderMatchTest extends UnitTest {

  "An order match" should "provide the match participants" in {
    val buyer = PeerConnection("buyer")
    val seller = PeerConnection("seller")
    val orderMatch = OrderMatch("some-exchange", 10 BTC, 1000 EUR, buyer, seller)
    orderMatch.participants should be (Set(buyer, seller))
  }
}
