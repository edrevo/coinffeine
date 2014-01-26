package com.bitwise.bitmarket.market

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.bitwise.bitmarket.common.currency.CurrencyCode.{EUR, USD}
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.protocol.{OrderMatch, Ask, Bid}

class OrderBookTest extends FlatSpec with MustMatchers {

  val sequentialIds = Stream.from(1).map(_.toString)

  "An order book" must "require asks to be sorted" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq.empty, asks = Seq(
        Ask(BtcAmount(1), EUR(20), "seller1"),
        Ask(BtcAmount(1), EUR(10), "seller2")
      ))
    } must produce [IllegalArgumentException]
    ex.toString must include ("Asks must be sorted")
  }

  it must "require a single order per requester" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq.empty, asks = Seq(
        Ask(BtcAmount(1), EUR(10), "repeated"),
        Ask(BtcAmount(1), EUR(20), "repeated")
      ))
    } must produce [IllegalArgumentException]
    ex.toString must include ("Requesters with multiple orders: repeated")
  }

  it must "require prices to be in one currency" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq.empty, asks = Seq(
        Ask(BtcAmount(1), EUR(20), "seller1"),
        Ask(BtcAmount(1), USD(10), "seller2")
      ))
    } must produce [IllegalArgumentException]
    ex.toString must include ("A currency (USD) other than EUR was used")
  }

  it must "require bids to be sorted" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq(
        Bid(BtcAmount(1), EUR(10), "buyer1"),
        Bid(BtcAmount(1), EUR(20), "buyer2")
      ), asks = Seq.empty)
    } must produce [IllegalArgumentException]
    ex.toString must include ("Bids must be sorted")
  }

  it must "detect a cross when a bid price is greater than an ask one" in {
    OrderBook.empty(EUR.currency).hasCross must be (false)
    OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "buyer")),
      Seq(Ask(BtcAmount(2), EUR(25), "seller"))
    ).hasCross must be (false)
    OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "buyer")),
      Seq(Ask(BtcAmount(2), EUR(15), "seller"))
    ).hasCross must be (true)
  }

  it must "quote a spread" in {
    OrderBook.empty(EUR.currency).spread must be ((None, None))
    OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "buyer")),
      Seq(Ask(BtcAmount(2), EUR(25), "seller"))
    ).spread must be (Some(EUR(20)), Some(EUR(25)))
  }

  it must "insert orders prioritized by price" in {
    val orders = Seq(
      Bid(BtcAmount(0.5), EUR(980), "bid3"),
      Ask(BtcAmount(0.5), EUR(930), "ask3"),
      Bid(BtcAmount(10), EUR(950), "bid1"),
      Ask(BtcAmount(10), EUR(940), "ask1"),
      Ask(BtcAmount(0.5), EUR(930), "ask4"),
      Bid(BtcAmount(0.5), EUR(980), "bid4"),
      Bid(BtcAmount(1), EUR(950), "bid2"),
      Ask(BtcAmount(1), EUR(940), "ask2")
    )
    val initialBook: OrderBook = OrderBook.empty(EUR.currency)
    val finalBook = orders.foldLeft(initialBook)(_.placeOrder(_))
    finalBook must be (OrderBook(
      EUR.currency,
      Seq(
        Bid(BtcAmount(0.5), EUR(980), "bid3"),
        Bid(BtcAmount(0.5), EUR(980), "bid4"),
        Bid(BtcAmount(10), EUR(950), "bid1"),
        Bid(BtcAmount(1), EUR(950), "bid2")
      ),
      Seq(
        Ask(BtcAmount(0.5), EUR(930), "ask3"),
        Ask(BtcAmount(0.5), EUR(930), "ask4"),
        Ask(BtcAmount(10), EUR(940), "ask1"),
        Ask(BtcAmount(1), EUR(940), "ask2")
      )
    ))
  }

  it must "replace previous unresolved orders when inserting" in {
    val book = OrderBook(
      EUR.currency,
      Seq(
        Bid(BtcAmount(4), EUR(120), "user1"),
        Bid(BtcAmount(3), EUR(95), "user2")
      ),
      Seq(Ask(BtcAmount(3), EUR(125), "user3"))
    )
    val user2Update = book.placeOrder(Bid(BtcAmount(3), EUR(120), "user2"))
    user2Update must be (book.copy(bids = Seq(
      Bid(BtcAmount(4), EUR(120), "user1"),
      Bid(BtcAmount(3), EUR(120), "user2")
    )))
    val user1Update = book.placeOrder(Ask(BtcAmount(1), EUR(115), "user1"))
    user1Update must be (book.copy(bids = book.bids.tail, asks = Seq(
      Ask(BtcAmount(1), EUR(115), "user1"),
      Ask(BtcAmount(3), EUR(125), "user3")
    )))
  }

  it must "cancel orders by requester" in {
    val book = OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "buyer")),
      Seq(Ask(BtcAmount(2), EUR(25), "seller"))
    )
    book.cancelOrder("buyer") must be (book.copy(bids = Seq.empty))
    book.cancelOrder("unknown") must be (book)
  }

  it must "be cleared with no changes when there is no cross" in {
    val book = OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "buyer")),
      Seq(Ask(BtcAmount(2), EUR(25), "seller"))
    )
    book.clearMarket(sequentialIds) must be ((book, Seq.empty))
  }

  it must "be cleared with a cross when two orders match perfectly" in {
    val book = OrderBook(
      EUR.currency,
      Seq(
        Bid(BtcAmount(2), EUR(25), "buyer"),
        Bid(BtcAmount(1), EUR(20), "other buyer")
      ),
      Seq(Ask(BtcAmount(2), EUR(25), "seller"))
    )
    val clearedBook = OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "other buyer")),
      Seq.empty
    )
    val cross = OrderMatch("1", BtcAmount(2), EUR(25), "buyer", "seller")
    book.clearMarket(sequentialIds) must be ((clearedBook, Seq(cross)))
  }

  it must "use the midpoint price" in {
    val book = OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(1), EUR(20), "buyer")),
      Seq(Ask(BtcAmount(1), EUR(10), "seller"))
    )
    val cross = OrderMatch("1", BtcAmount(1), EUR(15), "buyer", "seller")
    book.clearMarket(sequentialIds)._2 must be (Seq(cross))
  }

  it must "clear orders partially" in {
    val book = OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(2), EUR(25), "buyer")),
      Seq(Ask(BtcAmount(1), EUR(25), "seller"))
    )
    val clearedBook = OrderBook(EUR.currency, Seq(Bid(BtcAmount(1), EUR(25), "buyer")), Seq.empty)
    val cross = OrderMatch("1", BtcAmount(1), EUR(25), "buyer", "seller")
    book.clearMarket(sequentialIds) must be ((clearedBook, Seq(cross)))
  }

  it must "clear multiple orders against if necessary" in {
    val book = OrderBook(
      EUR.currency,
      Seq(Bid(BtcAmount(5), EUR(25), "buyer")),
      Seq(
        Ask(BtcAmount(2), EUR(15), "seller1"),
        Ask(BtcAmount(2), EUR(20), "seller2"),
        Ask(BtcAmount(2), EUR(25), "seller3")
      )
    )
    val clearedBook = OrderBook(
      EUR.currency,
      Seq.empty,
      Seq(Ask(BtcAmount(1), EUR(25), "seller3"))
    )
    val crosses = Seq(
      OrderMatch("1", BtcAmount(2), EUR(20), "buyer", "seller1"),
      OrderMatch("2", BtcAmount(2), EUR(22.5), "buyer", "seller2"),
      OrderMatch("3", BtcAmount(1), EUR(25), "buyer", "seller3")
    )
    book.clearMarket(sequentialIds) must be ((clearedBook, crosses))
  }
}
