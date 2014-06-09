package com.coinffeine.market

import com.coinffeine.common.{PeerConnection, UnitTest}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.Currency.Euro

class OrderBookTest extends UnitTest {

  def bid(btc: BigDecimal, eur: BigDecimal, by: String) =
    Position.bid(btc.BTC, eur.EUR, PeerConnection(by))

  def ask(btc: BigDecimal, eur: BigDecimal, by: String) =
    Position.ask(btc.BTC, eur.EUR, PeerConnection(by))

  val buyer = PeerConnection("buyer")
  val seller = PeerConnection("seller")

  "An order book" should "detect a cross when a bid price is greater than an ask one" in {
    OrderBook.empty(Euro) should not be 'crossed
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
    OrderBook.empty(Euro).spread should be ((None, None))
    OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    ).spread should be (Some(20 EUR), Some(25 EUR))
  }

  it should "keep previous unresolved orders when inserting a new one" in {
    val book = OrderBook(
      bid(btc = 4, eur = 120, by = "user1"),
      bid(btc = 3, eur = 95, by = "user2"),
      ask(btc = 3, eur = 125, by = "user3")
    )
    val user2 = PeerConnection("user2")
    val updatedBook = book.addPosition(Position.bid(3.BTC, 120.EUR, user2))
    updatedBook.positions.count(p => p.requester == user2) should be (2)
  }

  it should "cancel positions by requester" in {
    val book = OrderBook(
      bid(btc = 1, eur = 20, by = "buyer"),
      bid(btc = 1, eur = 22, by = "buyer"),
      ask(btc = 2, eur = 25, by = "seller")
    )
    book.cancelAllPositions(buyer) should be (OrderBook(
      ask(btc = 2, eur = 25, by = "seller")
    ))
    book.cancelAllPositions(PeerConnection("unknown")) should be (book)
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
    val cross = Cross(1.BTC, 15.EUR, buyer, seller)
    book.clearMarket._2 should be (Seq(cross))
  }

  it should "clear orders partially" in {
    val book = OrderBook(
      bid(btc = 2, eur = 25, by = "buyer"),
      ask(btc = 1, eur = 25, by = "seller")
    )
    val clearedBook = OrderBook(bid(btc = 1, eur = 25, by = "buyer"))
    val cross = Cross(1.BTC, 25.EUR, buyer, seller)
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
