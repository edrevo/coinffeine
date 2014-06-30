package com.coinffeine.common.exchange.impl

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction}
import com.coinffeine.common.exchange.Both
import com.google.bitcoin.core.Transaction.SigHash

import scala.collection.JavaConversions._

class ValidDepositsTest extends ExchangeTest {

  "Valid deposits" should "not be built from an invalid buyer commitment transaction" in new Fixture {
    ValidDeposits.validate(Both(
      buyer = ImmutableTransaction(invalidFundsCommitment),
      seller = sellerHandshake.myDeposit
    ), exchange) should be ('failure)
  }

  it should "not be built from an invalid seller commitment transaction" in new Fixture {
    ValidDeposits.validate(Both(
      buyer = buyerHandshake.myDeposit,
      seller = ImmutableTransaction(invalidFundsCommitment)
    ), exchange) should be ('failure)
  }

  trait Fixture extends BuyerHandshake with SellerHandshake {
    val invalidFundsCommitment = new MutableTransaction(exchange.parameters.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput(5.BTC.asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
  }
}
