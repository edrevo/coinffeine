package com.coinffeine.market

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.currency.Implicits._

class PositionTest extends UnitTest {

  "A position" should "be folded depending on its type" in  {
    Position.bid(1.BTC, 100.EUR, PeerConnection("user")).fold(
      bid = p => p.amount,
      ask = p => -p.amount
    ) should be (1.BTC)
    Position.ask(1.BTC, 100.EUR, PeerConnection("user")).fold(
      bid = p => p.amount,
      ask = p => -p.amount
    ) should be (-1.BTC)
  }
}
