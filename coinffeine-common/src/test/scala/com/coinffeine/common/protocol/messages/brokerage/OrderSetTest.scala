package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.UnitTest
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.Currency.Implicits._

class OrderSetTest extends UnitTest {

  val eurMarket = Market(Euro)
  val volume = VolumeByPrice(100.EUR -> 1.BTC)

  "An order set" should "have its highest bid lower than its lowest ask" in {
    the [IllegalArgumentException] thrownBy {
      OrderSet(eurMarket, bids = volume, asks = volume)
    } should have message "requirement failed: Bids and asks are crossed"
  }

  it should "be empty only if has no bids nor asks" in {
    val emptySet = OrderSet.empty(eurMarket)
    emptySet should be ('empty)
    emptySet.copy(bids = volume) should not be 'empty
    emptySet.copy(asks = volume) should not be 'empty
  }

  it should "be extended with new orders" in {
    OrderSet.empty(eurMarket)
      .addOrder(Bid, 10.BTC, 1000.EUR)
      .addOrder(Ask, 1.BTC, 1100.EUR)
      .addOrder(Bid, 1.BTC, 1000.EUR) should be (OrderSet(
      market = eurMarket,
      bids = VolumeByPrice(1000.EUR -> 11.BTC),
      asks = VolumeByPrice(1100.EUR -> 1.BTC)
    ))
  }
}
