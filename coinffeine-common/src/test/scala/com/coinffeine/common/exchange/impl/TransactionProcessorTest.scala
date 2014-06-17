package com.coinffeine.common.exchange.impl

import com.google.bitcoin.core.Transaction.SigHash

import scala.collection.JavaConverters._

import com.google.bitcoin.core.TransactionOutput

import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._

class TransactionProcessorTest extends BitcoinjTest {

  import com.coinffeine.common.exchange.impl.Samples._

  val signatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)

  "Multisign transaction creation" should "fail if the amount to commit is less or equal to zero" in {
    val userWallet = createWallet(exchange.buyer.bitcoinKey, 5.BTC)
    an [IllegalArgumentException] should be thrownBy {
      TransactionProcessor.createMultisignDeposit(userWallet, 0.BTC, signatures, network)
    }
  }

  it should "commit the correct amount when the input exceeds the amount needed" in {
    val userWallet = createWallet(exchange.buyer.bitcoinKey, 5.BTC)
    val commitmentAmount = 2 BTC
    val transaction = TransactionProcessor.createMultisignDeposit(
        userWallet, commitmentAmount, signatures, network
    )
    Currency.Bitcoin.fromSatoshi(transaction.getValue(userWallet)) should be (-commitmentAmount)
  }

  it should "commit the correct amount when the input matches the amount needed" in {
    val commitmentAmount = 2 BTC
    val userWallet = createWallet(exchange.buyer.bitcoinKey, commitmentAmount)
    val transaction = TransactionProcessor.createMultisignDeposit(
      userWallet, commitmentAmount, signatures, network
    )
    Currency.Bitcoin.fromSatoshi(transaction.getValue(userWallet)) should be (-commitmentAmount)
  }

  it should "produce a TX ready for broadcast and insertion into the blockchain" in {
    val commitmentAmount = 2 BTC
    val userWallet = createWallet(exchange.buyer.bitcoinKey, commitmentAmount)
    val transaction = TransactionProcessor.createMultisignDeposit(
      userWallet, commitmentAmount, signatures, network
    )
    sendToBlockChain(transaction)
    Currency.Bitcoin.fromSatoshi(userWallet.getBalance) should be (0 BTC)
  }

  "Unsigned transaction creation" should "create valid transactions except for the signature" in {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 1.BTC)
    val sellerWallet = createWallet(exchange.seller.bitcoinKey)
    val transaction = TransactionProcessor.createUnsignedTransaction(
      inputs = buyerWallet.calculateAllSpendCandidates(true).asScala,
      outputs = Seq(exchange.seller.bitcoinKey -> 0.8.BTC, exchange.buyer.bitcoinKey -> 0.2.BTC),
      network = network
    )
    transaction.signInputs(SigHash.ALL, buyerWallet)
    sendToBlockChain(transaction)
    Currency.Bitcoin.fromSatoshi(buyerWallet.getBalance) should be (0.2.BTC)
    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (0.8.BTC)
  }
}
