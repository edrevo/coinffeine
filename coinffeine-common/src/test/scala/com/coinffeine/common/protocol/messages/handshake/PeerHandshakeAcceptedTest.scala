package com.coinffeine.common.protocol.messages.handshake

import java.math.BigInteger

import com.coinffeine.common.{EqualityBehaviors, UnitTest}
import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.exchange.Exchange

class PeerHandshakeAcceptedTest extends UnitTest with EqualityBehaviors {
  val sig1 = new TransactionSignature(BigInteger.valueOf(0), BigInteger.valueOf(1))
  val sig2 = new TransactionSignature(BigInteger.valueOf(1), BigInteger.valueOf(0))

  "Step signatures" should behave like respectingEqualityProperties(equivalenceClasses = Seq(
    Seq(
      PeerHandshakeAccepted(Exchange.Id("id"), sig1),
      PeerHandshakeAccepted(Exchange.Id("id"), sig1)
    ),
    Seq(PeerHandshakeAccepted(Exchange.Id("id2"), sig1)),
    Seq(PeerHandshakeAccepted(Exchange.Id("id1"), sig2))
  ))
}
