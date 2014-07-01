package com.coinffeine.common.exchange.impl

import scala.collection.JavaConversions._

import com.google.bitcoin.core.Transaction.SigHash

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.{ImmutableTransaction, MutableTransaction}
import com.coinffeine.common.exchange.Both

class DepositValidatorTest extends ExchangeTest {

  "Valid deposits" should "not be built from an invalid buyer commitment transaction" in
    new Fixture {
      validator.validate(Both(
        buyer = ImmutableTransaction(invalidFundsCommitment),
        seller = sellerHandshake.myDeposit
      )) should be ('failure)
  }

  it should "not be built from an invalid seller commitment transaction" in new Fixture {
    validator.validate(Both(
      buyer = buyerHandshake.myDeposit,
      seller = ImmutableTransaction(invalidFundsCommitment)
    )) should be ('failure)
  }

  trait Fixture extends BuyerHandshake with SellerHandshake {
    val invalidFundsCommitment = new MutableTransaction(exchange.parameters.network)
    invalidFundsCommitment.addInput(sellerWallet.calculateAllSpendCandidates(true).head)
    invalidFundsCommitment.addOutput(5.BTC.asSatoshi, sellerWallet.getKeys.head)
    invalidFundsCommitment.signInputs(SigHash.ALL, sellerWallet)
    val validator = new DepositValidator(exchange)
  }
}
