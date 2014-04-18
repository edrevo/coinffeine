package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.UnitTest

class OrderTest extends UnitTest {

  "An order" should "correspond to a non-negative amount" in {
    val ex = the [IllegalArgumentException] thrownBy {
      Order(Bid, BtcAmount(0), EUR(550))
    }
    ex.getMessage should include ("Amount ordered must be strictly positive")
  }

  it should "have non-negative price" in {
    val ex = the [IllegalArgumentException] thrownBy {
      Order(Bid, BtcAmount(10), EUR(0))
    }
    ex.getMessage should include ("Price must be strictly positive")
  }

  "A bid" should "be sorted only by decreasing price" in {
    Seq(
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(10), EUR(950)),
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(1), EUR(950))
    ).sorted should equal (Seq(
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(10), EUR(950)),
      Order(Bid, BtcAmount(1), EUR(950))
    ))
  }

  "An ask" should "be sorted only by increasing price" in {
    Seq(
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(10), EUR(940)),
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(1), EUR(940))
    ).sorted should equal (Seq(
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(10), EUR(940)),
      Order(Ask, BtcAmount(1), EUR(940))
    ))
  }
}
