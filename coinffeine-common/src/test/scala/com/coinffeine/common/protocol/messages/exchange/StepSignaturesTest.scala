package com.coinffeine.common.protocol.messages.exchange

import java.math.BigInteger

import com.coinffeine.common.{EqualityBehaviors, UnitTest}
import com.coinffeine.common.bitcoin.TransactionSignature
import com.coinffeine.common.exchange.{Both, Exchange}

class StepSignaturesTest extends UnitTest with EqualityBehaviors {

  val sig1 = new TransactionSignature(BigInteger.valueOf(0), BigInteger.valueOf(1))
  val sig2 = new TransactionSignature(BigInteger.valueOf(1), BigInteger.valueOf(0))

  "Step signatures" should behave like respectingEqualityProperties(equivalenceClasses = Seq(
    Seq(
      StepSignatures(Exchange.Id("id"), 1, Both(buyer = sig1, seller = sig2)),
      StepSignatures(Exchange.Id("id"), 1, Both(buyer = sig1, seller = sig2))
    ),
    Seq(
      StepSignatures(Exchange.Id("id"), 1, Both(buyer = sig1, seller = sig1)),
      StepSignatures(Exchange.Id("id"), 1, Both(buyer = sig1, seller = sig1))
    ),
    Seq(StepSignatures(Exchange.Id("id2"), 1, Both(buyer = sig1, seller = sig2))),
    Seq(StepSignatures(Exchange.Id("id"), 2, Both(buyer = sig1, seller = sig2)))
  ))
}
