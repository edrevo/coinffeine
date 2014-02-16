package com.coinffeine.common.protocol

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.PeerConnection

class OrderMatchTest extends FlatSpec with ShouldMatchers {

  "An order match" should "provide the match participants" in {
    val buyer = PeerConnection("buyer")
    val seller = PeerConnection("seller")
    val orderMatch = OrderMatch("some-exchange", BtcAmount(10), EUR(1000), buyer, seller)
    orderMatch.participants should be (Set(buyer, seller))
  }
}
