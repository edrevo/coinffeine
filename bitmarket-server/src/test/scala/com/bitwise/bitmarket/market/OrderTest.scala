package com.bitwise.bitmarket.market

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.protocol.{Order, Ask, Bid}

class OrderTest extends FlatSpec with MustMatchers {

  "A bid" must "be sorted only by decreasing price" in {
    Seq(
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(10), EUR(950)),
      Order(Bid, BtcAmount(0.5), EUR(980)),
      Order(Bid, BtcAmount(1), EUR(950))
    ).sorted(Order.DescendingPriceOrder) must equal (Seq(
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
    ).sorted(Order.AscendingPriceOrder) must equal (Seq(
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(0.5), EUR(930)),
      Order(Ask, BtcAmount(10), EUR(940)),
      Order(Ask, BtcAmount(1), EUR(940))
    ))
  }
}
