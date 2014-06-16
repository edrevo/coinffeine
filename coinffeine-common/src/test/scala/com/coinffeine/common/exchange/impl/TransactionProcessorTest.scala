package com.coinffeine.common.exchange.impl

import com.coinffeine.common.{Currency, BitcoinjTest}
import com.coinffeine.common.Currency.Implicits._

class TransactionProcessorTest extends BitcoinjTest {

  import com.coinffeine.common.exchange.impl.Samples._

  val signatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)

  "Multisign transaction creation" should "fail if the amount to commit is less or equal to zero" in {
    val userWallet = createWallet(exchange.buyer.bitcoinKey, 5.BTC)
    an [IllegalArgumentException] should be thrownBy {
      TransactionProcessor.createMultisignTransaction(userWallet, 0.BTC, signatures, network)
    }
  }

  it should "commit the correct amount when the input exceeds the amount needed" in {
    val userWallet = createWallet(exchange.buyer.bitcoinKey, 5.BTC)
    val commitmentAmount = 2 BTC
    val transaction = TransactionProcessor.createMultisignTransaction(
        userWallet, commitmentAmount, signatures, network
    )
    Currency.Bitcoin.fromSatoshi(transaction.getValue(userWallet)) should be (-commitmentAmount)
  }

  it should "commit the correct amount when the input matches the amount needed" in {
    val commitmentAmount = 2 BTC
    val userWallet = createWallet(exchange.buyer.bitcoinKey, commitmentAmount)
    val transaction = TransactionProcessor.createMultisignTransaction(
      userWallet, commitmentAmount, signatures, network
    )
    Currency.Bitcoin.fromSatoshi(transaction.getValue(userWallet)) should be (-commitmentAmount)
  }

  it should "produce a TX ready for broadcast and insertion into the blockchain" in {
    val commitmentAmount = 2 BTC
    val userWallet = createWallet(exchange.buyer.bitcoinKey, commitmentAmount)
    val transaction = TransactionProcessor.createMultisignTransaction(
      userWallet, commitmentAmount, signatures, network
    )
    sendToBlockChain(transaction)
    Currency.Bitcoin.fromSatoshi(userWallet.getBalance) should be (0 BTC)
  }
}
