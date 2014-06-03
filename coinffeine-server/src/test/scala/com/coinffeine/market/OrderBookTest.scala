package com.coinffeine.market

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.{EUR, USD}
import com.coinffeine.common.currency.Implicits._
import com.coinffeine.common.protocol.messages.brokerage.{Ask, Bid, Order}

class OrderBookTest extends UnitTest {

  def ask(btc: BigDecimal, eur: BigDecimal, by: String): Position =
    Position(PeerConnection(by), Order(Ask, BtcAmount(btc), EUR(eur)))

  def bid(btc: BigDecimal, eur: BigDecimal, by: String): Position =
    Position(PeerConnection(by), Order(Bid, BtcAmount(btc), EUR(eur)))

  val buyer = PeerConnection("buyer")
  val seller = PeerConnection("seller")

  "An order book" should "require prices to be in one currency" in {
    val ex = the [IllegalArgumentException] thrownBy {
      OrderBook(
        ask(btc = 1, eur = 20, by = "seller1"),
        Position(seller, Order(Ask, BtcAmount(1), USD(10)))
      )
    }
    ex.toString should include ("Cannot mix EUR with USD")
  }

  it should "detect a cross when a bid price is greater than an ask one" in {
    OrderBook.empty(EUR.currency) should not be 'crossed
    OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    ) should not be 'crossed
    OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 2, eur = 15, by = "seller")
    ) should be ('crossed)
  }

  it should "quote a spread" in {
    OrderBook.empty(EUR.currency).spread should be ((None, None))
    OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    ).spread should be (Some(EUR(20)), Some(EUR(25)))
  }

  it should "keep previous unresolved orders when inserting a new one" in {
    val book = OrderBook(
      bid(btc = 4, eur = 120, by = "user1"),
      bid(btc = 3, eur = 95, by = "user2"),
      ask(btc = 3, eur = 125, by = "user3")
    )
    val user2 = PeerConnection("user2")
    val updatedBook = book.placeOrder(user2, Order(Bid, 3.BTC, 120.EUR))
    updatedBook.positions.count(p => p.requester == user2) should be (2)
  }

  it should "cancel positions by requester" in {
    val book = OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      bid(btc = 1, eur = 22, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    )
    book.cancelPositions(buyer) should be (OrderBook(
      ask(btc = 2, eur = 25, by = "seller")
    ))
    book.cancelPositions(PeerConnection("unknown")) should be (book)
  }

  it should "cancel individual orders" in {
    val book = OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      bid(btc = 1, eur = 22, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    )
    book.cancelPosition(bid(btc = 1, eur = 22, by = "buyer")) should be (OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    ))
    book.cancelPosition(bid(btc = 1, eur = 22, by = "unknown")) should be (book)
  }

  it should "be cleared with no changes when there is no cross" in {
    val book = OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    )
    book.clearMarket should be ((book, Seq.empty))
  }

  it should "be cleared with a cross when two orders match perfectly" in {
    val book = OrderBook(
      bid(btc = 2, eur = 25, by = "buyer"),
      bid(btc = 1, eur = 20, by = "other buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    )
    val clearedBook = OrderBook(
      bid(btc = 1, eur = 20, by = "other buyer")
    )
    val cross = Cross(2.BTC, 25.EUR, buyer, seller)
    book.clearMarket should be ((clearedBook, Seq(cross)))
  }

  it should "use the midpoint price" in {
    val book = OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 1, eur = 10, by = "seller")
    )
    val cross = Cross(BtcAmount(1), EUR(15), buyer, seller)
    book.clearMarket._2 should be (Seq(cross))
  }

  it should "clear orders partially" in {
    val book = OrderBook(
      bid(btc = 2, eur = 25, by = "buyer"),
      ask(btc = 1, eur = 25, by = "seller")
    )
    val clearedBook = OrderBook(bid(btc = 1, eur = 25, by = "buyer"))
    val cross = Cross(BtcAmount(1), EUR(25), buyer, seller)
    book.clearMarket should be ((clearedBook, Seq(cross)))
  }

  it should "clear multiple orders against one if necessary" in {
    val book = OrderBook(
      bid(btc = 5, eur = 25, by = "buyer"),
      ask(btc = 2, eur = 15, by = "seller1"),
      ask(btc = 2, eur = 20, by = "seller2"),
      ask(btc = 2, eur = 25, by = "seller3")
    )
    val clearedBook = OrderBook(ask(btc = 1, eur = 25, by = "seller3"))
    val crosses = Seq(
      Cross(2.BTC, 20.EUR, buyer, PeerConnection("seller1")),
      Cross(2.BTC, 22.5.EUR, buyer, PeerConnection("seller2")),
      Cross(1.BTC, 25.EUR, buyer, PeerConnection("seller3"))
    )
    book.clearMarket should be ((clearedBook, crosses))
  }
}
