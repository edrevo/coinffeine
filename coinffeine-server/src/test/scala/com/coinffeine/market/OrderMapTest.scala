package com.coinffeine.market

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.currency.CurrencyCode._
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.messages.brokerage.Bid

class OrderMapTest extends UnitTest {

  val client1 = PeerConnection("client1")
  val client2 = PeerConnection("client2")

  "An order map" should "allow only a FIAT currency" in {
    val ex = the [IllegalArgumentException] thrownBy {
      OrderMap(
        Position.bid(2.BTC, 3.EUR, client1),
        Position.bid(1.BTC, 5.USD, client2)
      )
    }
    ex.toString should include ("Cannot mix EUR and USD values")
  }

  it should "reject positions placed with other currencies" in {
    val map = OrderMap.empty(Bid, EUR.currency)
    an [IllegalArgumentException] should be thrownBy {
      map.addPosition(Position.bid(2.BTC, 3.USD, client1))
    }
  }

  it should "remove the first order when clearing its exact amount of bitcoins" in {
    val map = OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    )
    map.removeAmount(2.BTC) should be (OrderMap(
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    ))
  }

  it should "remove several orders when one is not enough amount" in {
    val map = OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    )
    map.removeAmount(5.BTC) should be (OrderMap(
      Position.bid(1.BTC, 2.7.EUR, client1)
    ))
  }

  it should "remove orders partially when amounts to remove doesn't match" in {
    val map = OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    )
    map.removeAmount(4.BTC) should be (OrderMap(
      Position.bid(1.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    ))
  }

  it should "remove orders honoring queue order when several orders share price" in {
    val map = OrderMap(
      Position.bid(2.BTC, 1.EUR, client1),
      Position.bid(2.BTC, 1.EUR, client2)
    )
    map.removeAmount(2.BTC) should be (OrderMap(
      Position.bid(2.BTC, 1.EUR, client2)
    ))
  }

  it should "cancel all orders of a requester" in {
    val map = OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    )
    map.cancelPositions(client1) should be (OrderMap(
      Position.bid(3.BTC, 2.9.EUR, client2)
    ))
  }

  it should "cancel a specific position" in {
    val map = OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    )
    map.cancelPosition(Position.bid(2.BTC, 3.EUR, client1)) should be (OrderMap(
      Position.bid(3.BTC, 2.9.EUR, client2),
      Position.bid(1.BTC, 2.7.EUR, client1)
    ))
  }

  it should "cancel the least prioritized position when duplicated" in {
    val map = OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 3.EUR, client2),
      Position.bid(2.BTC, 3.EUR, client1)
    )
    map.cancelPosition(Position.bid(2.BTC, 3.EUR, client1)) should be (OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(3.BTC, 3.EUR, client2)
    ))
  }

  it should "throw when removing amounts greater than the sum of the orders" in {
    val map = OrderMap(
      Position.bid(2.BTC, 1.EUR, client1),
      Position.bid(3.BTC, 2.EUR, client2)
    )
    an [IllegalArgumentException] should be thrownBy {
      map.removeAmount(6.BTC)
    }
  }

  "A bid order map" should "order positions by descending price" in {
    OrderMap(
      Position.bid(2.BTC, 3.EUR, client1),
      Position.bid(1.BTC, 5.EUR, client2)
    ).positions should be (Seq(
      Position.bid(1.BTC, 5.EUR, client2),
      Position.bid(2.BTC, 3.EUR, client1)
    ))
  }

  "An ask order map" should "order positions by ascending price" in {
    OrderMap(
      Position.ask(2.BTC, 3.EUR, client1),
      Position.ask(1.BTC, 5.EUR, client2)
    ).positions should be (Seq(
      Position.ask(2.BTC, 3.EUR, client1),
      Position.ask(1.BTC, 5.EUR, client2)
    ))
  }
}
