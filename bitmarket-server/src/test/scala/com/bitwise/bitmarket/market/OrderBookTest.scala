package com.bitwise.bitmarket.market

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.CurrencyCode.{EUR, USD}
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.protocol._

class OrderBookTest extends FlatSpec with MustMatchers {

  val sequentialIds = Stream.from(1).map(_.toString)

  def askPosition(btc: BigDecimal, eur: BigDecimal, by: String): Position =
    Position(PeerConnection(by), Order(Ask, BtcAmount(btc), EUR(eur)))

  def bidPosition(btc: BigDecimal, eur: BigDecimal, by: String): Position =
    Position(PeerConnection(by), Order(Bid, BtcAmount(btc), EUR(eur)))

  "An order book" must "require asks to be sorted" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq.empty, asks = Seq(
        askPosition(btc = 1, eur = 20, by = "seller1"),
        askPosition(btc = 1, eur = 10, by = "seller2")
      ))
    } must produce [IllegalArgumentException]
    ex.toString must include ("Asks must be sorted")
  }

  it must "require a single order per requester" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq.empty, asks = Seq(
        askPosition(btc = 1, eur = 10, by = "repeated"),
        askPosition(btc = 1, eur = 20, by = "repeated")
      ))
    } must produce [IllegalArgumentException]
    ex.toString must include (s"Requesters with multiple orders: ${PeerConnection("repeated")}")
  }

  it must "require prices to be in one currency" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq.empty, asks = Seq(
        askPosition(btc = 1, eur = 20, by = "seller1"),
        Position(PeerConnection("seller"), Order(Ask, BtcAmount(1), USD(10)))
      ))
    } must produce [IllegalArgumentException]
    ex.toString must include ("A currency (USD) other than EUR was used")
  }

  it must "require bids to be sorted" in {
    val ex = evaluating {
      OrderBook(EUR.currency, bids = Seq(
        bidPosition(btc = 1, eur = 10, by = "buyer1"),
        bidPosition(btc = 1, eur = 20, by = "buyer2")
      ), asks = Seq.empty)
    } must produce [IllegalArgumentException]
    ex.toString must include ("Bids must be sorted")
  }

  it must "detect a cross when a bid price is greater than an ask one" in {
    OrderBook.empty(EUR.currency).hasCross must be (false)
    OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "buyer")),
      Seq(askPosition(btc = 2, eur = 25, by = "seller"))
    ).hasCross must be (false)
    OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "buyer")),
      Seq(askPosition(btc = 2, eur = 15, by = "seller"))
    ).hasCross must be (true)
  }

  it must "quote a spread" in {
    OrderBook.empty(EUR.currency).spread must be ((None, None))
    OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "buyer")),
      Seq(askPosition(btc = 2, eur = 25, by = "seller"))
    ).spread must be (Some(EUR(20)), Some(EUR(25)))
  }

  it must "insert orders prioritized by price" in {
    val orders = Seq(
      bidPosition(btc = 0.5, eur = 980, by = "bid3"),
      askPosition(btc = 0.5, eur = 930, by = "ask3"),
      bidPosition(btc = 10, eur = 950, by = "bid1"),
      askPosition(btc = 10, eur = 940, by = "ask1"),
      askPosition(btc = 0.5, eur = 930, by = "ask4"),
      bidPosition(btc = 0.5, eur = 980, by = "bid4"),
      bidPosition(btc = 1, eur = 950, by = "bid2"),
      askPosition(btc = 1, eur = 940, by = "ask2")
    )
    val initialBook: OrderBook = OrderBook.empty(EUR.currency)
    val finalBook = orders.foldLeft(initialBook) {
      case (book, Position(requester, order)) => book.placeOrder(requester, order)
    }
    finalBook must be (OrderBook(
      EUR.currency,
      Seq(
        bidPosition(btc = 0.5, eur = 980, by = "bid3"),
        bidPosition(btc = 0.5, eur = 980, by = "bid4"),
        bidPosition(btc = 10, eur = 950, by = "bid1"),
        bidPosition(btc = 1, eur = 950, by = "bid2")
      ),
      Seq(
        askPosition(btc = 0.5, eur = 930, by = "ask3"),
        askPosition(btc = 0.5, eur = 930, by = "ask4"),
        askPosition(btc = 10, eur = 940, by = "ask1"),
        askPosition(btc = 1, eur = 940, by = "ask2")
      )
    ))
  }

  it must "replace previous unresolved orders when inserting" in {
    val book = OrderBook(
      EUR.currency,
      Seq(
        bidPosition(btc = 4, eur = 120, by = "user1"),
        bidPosition(btc = 3, eur = 95, by = "user2")
      ),
      Seq(askPosition(btc = 3, eur = 125, by = "user3"))
    )
    val user2Update = book.placeOrder(PeerConnection("user2"), Order(Bid, BtcAmount(3), EUR(120)))
    user2Update must be (book.copy(bids = Seq(
      bidPosition(btc = 4, eur = 120, by = "user1"),
      bidPosition(btc = 3, eur = 120, by = "user2")
    )))
    val user1Update = book.placeOrder(PeerConnection("user1"), Order(Ask, BtcAmount(1), EUR(115)))
    user1Update must be (book.copy(bids = book.bids.tail, asks = Seq(
      askPosition(btc = 1, eur = 115, by = "user1"),
      askPosition(btc = 3, eur = 125, by = "user3")
    )))
  }

  it must "cancel orders by requester" in {
    val book = OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "buyer")),
      Seq(askPosition(btc = 2, eur = 25, by = "seller"))
    )
    book.cancelOrder(PeerConnection("buyer")) must be (book.copy(bids = Seq.empty))
    book.cancelOrder(PeerConnection("unknown")) must be (book)
  }

  it must "be cleared with no changes when there is no cross" in {
    val book = OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "buyer")),
      Seq(askPosition(btc = 2, eur = 25, by = "seller"))
    )
    book.clearMarket(sequentialIds) must be ((book, Seq.empty))
  }

  it must "be cleared with a cross when two orders match perfectly" in {
    val book = OrderBook(
      EUR.currency,
      Seq(
        bidPosition(btc = 2, eur = 25, by = "buyer"),
        bidPosition(btc = 1, eur = 20, by = "other buyer")
      ),
      Seq(askPosition(btc = 2, eur = 25, by = "seller"))
    )
    val clearedBook = OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "other buyer")),
      Seq.empty
    )
    val cross =
      OrderMatch("1", BtcAmount(2), EUR(25), PeerConnection("buyer"), PeerConnection("seller"))
    book.clearMarket(sequentialIds) must be ((clearedBook, Seq(cross)))
  }

  it must "use the midpoint price" in {
    val book = OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 1, eur = 20, by = "buyer")),
      Seq(askPosition(btc = 1, eur = 10, by = "seller"))
    )
    val cross =
      OrderMatch("1", BtcAmount(1), EUR(15), PeerConnection("buyer"), PeerConnection("seller"))
    book.clearMarket(sequentialIds)._2 must be (Seq(cross))
  }

  it must "clear orders partially" in {
    val book = OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 2, eur = 25, by = "buyer")),
      Seq(askPosition(btc = 1, eur = 25, by = "seller"))
    )
    val clearedBook =
      OrderBook(EUR.currency, Seq(bidPosition(btc = 1, eur = 25, by = "buyer")), Seq.empty)
    val cross =
      OrderMatch("1", BtcAmount(1), EUR(25), PeerConnection("buyer"), PeerConnection("seller"))
    book.clearMarket(sequentialIds) must be ((clearedBook, Seq(cross)))
  }

  it must "clear multiple orders against one if necessary" in {
    val book = OrderBook(
      EUR.currency,
      Seq(bidPosition(btc = 5, eur = 25, by = "buyer")),
      Seq(
        askPosition(btc = 2, eur = 15, by = "seller1"),
        askPosition(btc = 2, eur = 20, by = "seller2"),
        askPosition(btc = 2, eur = 25, by = "seller3")
      )
    )
    val clearedBook = OrderBook(
      EUR.currency,
      Seq.empty,
      Seq(askPosition(btc = 1, eur = 25, by = "seller3"))
    )
    val crosses = Seq(
      OrderMatch("1", BtcAmount(2), EUR(20), PeerConnection("buyer"), PeerConnection("seller1")),
      OrderMatch("2", BtcAmount(2), EUR(22.5), PeerConnection("buyer"), PeerConnection("seller2")),
      OrderMatch("3", BtcAmount(1), EUR(25), PeerConnection("buyer"), PeerConnection("seller3"))
    )
    book.clearMarket(sequentialIds) must be ((clearedBook, crosses))
  }
}
