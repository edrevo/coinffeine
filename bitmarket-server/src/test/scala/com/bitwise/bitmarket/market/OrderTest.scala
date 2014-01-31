package com.bitwise.bitmarket.market

import org.scalatest.FlatSpec
import org.scalatest.matchers.MustMatchers

import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.protocol.{Ask, Bid}

class OrderTest extends FlatSpec with MustMatchers {

  "A bid" must "be sorted only by decreasing price" in {
    Seq(
      Bid(BtcAmount(0.5), EUR(980), PeerConnection("agent3")),
      Bid(BtcAmount(10), EUR(950), PeerConnection("agent1")),
      Bid(BtcAmount(0.5), EUR(980), PeerConnection("agent4")),
      Bid(BtcAmount(1), EUR(950), PeerConnection("agent2"))
    ).sorted must equal (Seq(
      Bid(BtcAmount(0.5), EUR(980), PeerConnection("agent3")),
      Bid(BtcAmount(0.5), EUR(980), PeerConnection("agent4")),
      Bid(BtcAmount(10), EUR(950), PeerConnection("agent1")),
      Bid(BtcAmount(1), EUR(950), PeerConnection("agent2"))
    ))
  }

  "An ask" must "be sorted only by increasing price" in {
    Seq(
      Ask(BtcAmount(0.5), EUR(930), PeerConnection("agent3")),
      Ask(BtcAmount(10), EUR(940), PeerConnection("agent1")),
      Ask(BtcAmount(0.5), EUR(930), PeerConnection("agent4")),
      Ask(BtcAmount(1), EUR(940), PeerConnection("agent2"))
    ).sorted must equal (Seq(
      Ask(BtcAmount(0.5), EUR(930), PeerConnection("agent3")),
      Ask(BtcAmount(0.5), EUR(930), PeerConnection("agent4")),
      Ask(BtcAmount(10), EUR(940), PeerConnection("agent1")),
      Ask(BtcAmount(1), EUR(940), PeerConnection("agent2"))
    ))
  }
}
