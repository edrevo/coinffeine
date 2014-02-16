package com.coinffeine.common

import org.scalatest.matchers.MustMatchers
import org.scalatest.FlatSpec

class PeerConnectionTest extends FlatSpec with MustMatchers {

  "A peer connection" must "be parsed from a chain with hostname and port" in {
    PeerConnection.parse("coinffeine://example.com:9876") must
      be (PeerConnection("example.com", 9876))
  }

  it must "be parsed from a chain with only a hostname" in {
    PeerConnection.parse("coinffeine://example.com") must
      be (PeerConnection("example.com", PeerConnection.DefaultPort))
  }

  it must "be parsed from a chain with hostname and port and a trailing slash" in {
    PeerConnection.parse("coinffeine://example.com:9876/") must
      be (PeerConnection("example.com", 9876))
  }

  it must "be parsed from a chain with only a hostname and a trailing slash" in {
    PeerConnection.parse("coinffeine://example.com/") must
      be (PeerConnection("example.com", PeerConnection.DefaultPort))
  }

  it must "throw when missing scheme prefix" in {
    evaluating(PeerConnection.parse("example.com:9876")) must produce [IllegalArgumentException]
  }
}
