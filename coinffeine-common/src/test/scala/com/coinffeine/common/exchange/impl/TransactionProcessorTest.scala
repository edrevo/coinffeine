package com.coinffeine.common.exchange.impl

import scala.collection.JavaConverters._

import com.google.bitcoin.core.Transaction.SigHash

import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._

class TransactionProcessorTest extends BitcoinjTest {

  import com.coinffeine.common.exchange.impl.Samples._

  val signatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)

  "Multisign transaction creation" should "fail if the amount to commit is less or equal to zero" in {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 5.BTC)
    val funds = TransactionProcessor.collectFunds(buyerWallet, 2.BTC).toSeq.map { output =>
      output -> exchange.buyer.bitcoinKey
    }
    an [IllegalArgumentException] should be thrownBy {
      TransactionProcessor.createMultiSignedDeposit(
        funds, 0.BTC, buyerWallet.getChangeAddress, signatures, network)
    }
  }

  it should "commit the correct amount when the input exceeds the amount needed" in {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 5.BTC)
    val commitmentAmount = 2 BTC
    val funds = TransactionProcessor.collectFunds(buyerWallet, 5.BTC).toSeq.map { output =>
      output -> exchange.buyer.bitcoinKey
    }
    val transaction = TransactionProcessor.createMultiSignedDeposit(
        funds, commitmentAmount, buyerWallet.getChangeAddress, signatures, network
    )
    Currency.Bitcoin.fromSatoshi(transaction.getValue(buyerWallet)) should be (-commitmentAmount)
  }

  it should "commit the correct amount when the input matches the amount needed" in {
    val commitmentAmount = 2 BTC
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, commitmentAmount)
    val funds = TransactionProcessor.collectFunds(buyerWallet, commitmentAmount).toSeq.map { output =>
      output -> exchange.buyer.bitcoinKey
    }
    val transaction = TransactionProcessor.createMultiSignedDeposit(
      funds, commitmentAmount, buyerWallet.getChangeAddress, signatures, network
    )
    Currency.Bitcoin.fromSatoshi(transaction.getValue(buyerWallet)) should be (-commitmentAmount)
  }

  it should "produce a TX ready for broadcast and insertion into the blockchain" in {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 2.BTC)
    val funds = TransactionProcessor.collectFunds(buyerWallet, 2.BTC).toSeq.map { output =>
      output -> exchange.buyer.bitcoinKey
    }
    val multiSigDeposit = TransactionProcessor.createMultiSignedDeposit(
      funds, 2.BTC, buyerWallet.getChangeAddress, signatures, network
    )
    sendToBlockChain(multiSigDeposit)
  }

  it should "spend a multisigned deposit" in {
    val buyerWallet = createWallet(exchange.buyer.bitcoinKey, 2.BTC)
    val sellerWallet = createWallet(exchange.seller.bitcoinKey)

    val funds = TransactionProcessor.collectFunds(buyerWallet, 2.BTC).toSeq.map { output =>
      output -> exchange.buyer.bitcoinKey
    }
    val multiSigDeposit = TransactionProcessor.createMultiSignedDeposit(
      funds, 2.BTC, buyerWallet.getChangeAddress, signatures, network
    )
    sendToBlockChain(multiSigDeposit)

    val tx = TransactionProcessor.createUnsignedTransaction(
      Seq(multiSigDeposit.getOutput(0)), Seq(exchange.seller.bitcoinKey -> 2.BTC), network
    )
    TransactionProcessor.setMultipleSignatures(tx, index = 0,
      TransactionProcessor.signMultiSignedOutput(tx, index = 0, exchange.buyer.bitcoinKey, signatures),
      TransactionProcessor.signMultiSignedOutput(tx, index = 0, exchange.seller.bitcoinKey, signatures)
    )
    sendToBlockChain(tx)
    Currency.Bitcoin.fromSatoshi(sellerWallet.getBalance) should be (2.BTC)
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
