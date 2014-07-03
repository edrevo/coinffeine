package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.{Order, Ask, Bid, UnitTest}

class OrderTest extends UnitTest {

  "An order" should "correspond to a non-negative amount" in {
    val ex = the [IllegalArgumentException] thrownBy {
      Order(Bid, 0 BTC, 550 EUR)
    }
    ex.getMessage should include ("Amount ordered must be strictly positive")
  }

  it should "have non-negative price" in {
    val ex = the [IllegalArgumentException] thrownBy {
      Order(Bid, 10 BTC, 0 EUR)
    }
    ex.getMessage should include ("Price must be strictly positive")
  }

  "A bid" should "be sorted only by decreasing price" in {
    Seq(
      Order(Bid, 0.5 BTC, 980 EUR),
      Order(Bid, 10 BTC, 950 EUR),
      Order(Bid, 0.5 BTC, 980 EUR),
      Order(Bid, 1 BTC, 950 EUR)
    ).sorted should equal (Seq(
      Order(Bid, 0.5 BTC, 980 EUR),
      Order(Bid, 0.5 BTC, 980 EUR),
      Order(Bid, 10 BTC, 950 EUR),
      Order(Bid, 1 BTC, 950 EUR)
    ))
  }

  "An ask" should "be sorted only by increasing price" in {
    Seq(
      Order(Ask, 0.5 BTC, 930 EUR),
      Order(Ask, 10 BTC, 940 EUR),
      Order(Ask, 0.5 BTC, 930 EUR),
      Order(Ask, 1 BTC, 940 EUR)
    ).sorted should equal (Seq(
      Order(Ask, 0.5 BTC, 930 EUR),
      Order(Ask, 0.5 BTC, 930 EUR),
      Order(Ask, 10 BTC, 940 EUR),
      Order(Ask, 1 BTC, 940 EUR)
    ))
  }
}
