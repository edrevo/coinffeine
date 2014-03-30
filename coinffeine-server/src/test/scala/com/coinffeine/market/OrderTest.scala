package com.coinffeine.market

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.protocol.messages.brokerage.{Order, Ask, Bid}

class OrderTest extends FlatSpec with MustMatchers {

  "An order" must "correspond to a non-negative amount" in {
    val ex = evaluating {
      Order(Bid, BtcAmount(0), EUR(550))
    } must produce [IllegalArgumentException]
    ex.getMessage must include ("Amount ordered must be strictly positive")
  }

  it must "have non-negative price" in {
    val ex = evaluating {
      Order(Bid, BtcAmount(10), EUR(0))
    } must produce [IllegalArgumentException]
    ex.getMessage must include ("Price must be strictly positive")
  }

  "A bid" must "be sorted only by decreasing price" in {
    Seq(
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(10), EUR(950)),
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(1), EUR(950))
    ).sorted must equal (Seq(
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(10), EUR(950)),
      Order(Bid, BtcAmount(1), EUR(950))
    ))
  }

  "An ask" must "be sorted only by increasing price" in {
    Seq(
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(10), EUR(940)),
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(1), EUR(940))
    ).sorted must equal (Seq(
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(10), EUR(940)),
      Order(Ask, BtcAmount(1), EUR(940))
    ))
  }
}
