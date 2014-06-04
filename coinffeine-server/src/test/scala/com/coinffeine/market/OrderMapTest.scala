package com.coinffeine.market

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.currency.CurrencyCode._
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid, Order}

class OrderMapTest extends UnitTest {

  val client1 = PeerConnection("client1")
  val client2 = PeerConnection("client2")

  "An order map" should "allow only bids or asks" in {
    val ex = the [IllegalArgumentException] thrownBy {
     OrderMap(
       Position(client1, Order(Bid, 2.BTC, 3.EUR)),
       Position(client2, Order(Ask, 1.BTC, 5.EUR))
     )
    }
    ex.toString should include ("Cannot mix Ask and Bid values")
  }

  it should "allow only a FIAT currency" in {
    val ex = the [IllegalArgumentException] thrownBy {
      OrderMap(
        Position(client1, Order(Bid, 2.BTC, 3.EUR)),
        Position(client2, Order(Bid, 1.BTC, 5.USD))
      )
    }
    ex.toString should include ("Cannot mix EUR and USD values")
  }

  it should "reject positions of other types" in {
    val map = OrderMap.empty(Bid, EUR.currency)
    an [IllegalArgumentException] should be thrownBy {
      map.addPosition(Position(client1, Order(Ask, 2.BTC, 3.EUR)))
    }
  }

  it should "reject positions placed with other currencies" in {
    val map = OrderMap.empty(Bid, EUR.currency)
    an [IllegalArgumentException] should be thrownBy {
      map.addPosition(Position(client1, Order(Bid, 2.BTC, 3.USD)))
    }
  }

  it should "remove the first order when clearing its exact amount of bitcoins" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    )
    map.removeAmount(2.BTC) should be (OrderMap(
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    ))
  }

  it should "remove several orders when one is not enough amount" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    )
    map.removeAmount(5.BTC) should be (OrderMap(
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    ))
  }

  it should "remove orders partially when amounts to remove doesn't match" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    )
    map.removeAmount(4.BTC) should be (OrderMap(
      Position(client2, Order(Bid, 1.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    ))
  }

  it should "remove orders honoring queue order when several orders share price" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 1.EUR)),
      Position(client2, Order(Bid, 2.BTC, 1.EUR))
    )
    map.removeAmount(2.BTC) should be (OrderMap(
      Position(client2, Order(Bid, 2.BTC, 1.EUR))
    ))
  }

  it should "cancel all orders of a requester" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    )
    map.cancelPositions(client1) should be (OrderMap(
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR))
    ))
  }

  it should "cancel a specific position" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    )
    map.cancelPosition(Position(client1, Order(Bid, 2.BTC, 3.EUR))) should be (OrderMap(
      Position(client2, Order(Bid, 3.BTC, 2.9.EUR)),
      Position(client1, Order(Bid, 1.BTC, 2.7.EUR))
    ))
  }

  it should "cancel the least prioritized position when duplicated" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 3.EUR)),
      Position(client1, Order(Bid, 2.BTC, 3.EUR))
    )
    map.cancelPosition(Position(client1, Order(Bid, 2.BTC, 3.EUR))) should be (OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 3.BTC, 3.EUR))
    ))
  }

  it should "throw when removing amounts greater than the sum of the orders" in {
    val map = OrderMap(
      Position(client1, Order(Bid, 2.BTC, 1.EUR)),
      Position(client2, Order(Bid, 3.BTC, 2.EUR))
    )
    an [IllegalArgumentException] should be thrownBy {
      map.removeAmount(6.BTC)
    }
  }

  "A bid order map" should "order positions by descending price" in {
    OrderMap(
      Position(client1, Order(Bid, 2.BTC, 3.EUR)),
      Position(client2, Order(Bid, 1.BTC, 5.EUR))
    ).positions should be (Seq(
      Position(client2, Order(Bid, 1.BTC, 5.EUR)),
      Position(client1, Order(Bid, 2.BTC, 3.EUR))
    ))
  }

  "An ask order map" should "order positions by ascending price" in {
    OrderMap(
      Position(client1, Order(Ask, 2.BTC, 3.EUR)),
      Position(client2, Order(Ask, 1.BTC, 5.EUR))
    ).positions should be (Seq(
      Position(client1, Order(Ask, 2.BTC, 3.EUR)),
      Position(client2, Order(Ask, 1.BTC, 5.EUR))
    ))
  }
}
