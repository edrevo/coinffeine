package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.UnitTest
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.messages.brokerage.OrderSet.Entry

class OrderSetTest extends UnitTest {

  "An order set" should "use only one currency" in {
    the [IllegalArgumentException] thrownBy {
      OrderSet(Market(EUR.currency), bids = Seq(Entry(1.BTC, 100.USD)))
    } should have message "requirement failed: Mixed currencies"
  }

  it should "have its highest bid lower than its lowest ask" in {
    the [IllegalArgumentException] thrownBy {
      OrderSet(
        market = Market(EUR.currency),
        bids = Seq(Entry(1.BTC, 100.EUR)),
        asks = Seq(Entry(1.BTC, 100.EUR))
      )
    } should have message "requirement failed: Bids and asks are crossed"
  }

  "An order set entry" should "correspond to a non-negative amount" in {
    val ex = the [IllegalArgumentException] thrownBy {
      Entry(0.BTC, 550.EUR)
    }
    ex.getMessage should include ("Amount ordered must be strictly positive")
  }

  it should "have non-negative price" in {
    val ex = the [IllegalArgumentException] thrownBy {
      Entry(10.BTC, 0.EUR)
    }
    ex.getMessage should include ("Price must be strictly positive")
  }

  it should "be extended with new orders" in {
    OrderSet(Market(EUR.currency))
      .addOrder(Bid, 10.BTC, 1000.EUR)
      .addOrder(Ask, 1.BTC, 1100.EUR)
      .addOrder(Bid, 1.BTC, 1000.EUR) should be (OrderSet(
      market = Market(EUR.currency),
      bids = Seq(Entry(11.BTC, 1000.EUR)),
      asks = Seq(Entry(1.BTC, 1100.EUR))
    ))
  }
}
