package com.bitwise.bitmarket.client.handshake

import scala.language.postfixOps
import scala.collection.JavaConversions._

import com.bitwise.bitmarket.client.{BitcoinjTest, Exchange}
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.BtcAmount.Implicits._

import com.google.bitcoin.core.{Transaction, VerificationException, ECKey}
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder
import org.scalatest.matchers.ShouldMatchers

class DefaultExchangeHandshakeTest extends BitcoinjTest with ShouldMatchers {
  val exchange = Exchange(
    id = "dummy",
    counterpart = PeerConnection("localhost", 1234),
    broker = PeerConnection("localhost", 1235),
    network = network,
    userKey = new ECKey(),
    counterpartKey = new ECKey(),
    exchangeAmount = 10 bitcoins,
    steps = 10,
    lockTime = 25)

  "The DefaultExchangeHandshake constructor" should "fail if the wallet does not contain the private key" in {
    evaluating {
      new DefaultExchangeHandshake(
        exchange = exchange,
        amountToCommit = 2 bitcoins,
        userWallet = createWallet) {}
    } should produce [IllegalArgumentException]
  }

  it should "fail if the amount to commit is less or equal to zero" in {
    val userWallet = createWallet(exchange.userKey, 5 bitcoins)
    evaluating {
      new DefaultExchangeHandshake(
        exchange = exchange,
        amountToCommit = 0 bitcoins,
        userWallet = userWallet) {}
    } should produce [IllegalArgumentException]
  }

  "The commitment transaction" should "commit the correct amount when the input exceeds the amount needed" in {
    val userWallet = createWallet(exchange.userKey, 1 bitcoin)
    sendMoneyToWallet(userWallet, 4 bitcoins)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    BtcAmount(handshake.commitmentTransaction.getValue(userWallet)) should be (-commitmentAmount)
  }

  it should "commit the correct amount when the input matches the amount needed" in {
    val userWallet = createWallet(exchange.userKey, 2 bitcoins)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    BtcAmount(handshake.commitmentTransaction.getValue(userWallet)) should be (-commitmentAmount)
  }

  it should "be ready for broadcast and insertion into the blockchain" in {
    val userWallet = createWallet(exchange.userKey, 2 bitcoins)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    BtcAmount(userWallet.getBalance) should be (0 bitcoins)
  }

  "The refund transaction" should "not be directly broadcastable to the blockchain" in {
    val userWallet = createWallet(exchange.userKey, 2 bitcoins)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    evaluating {
      sendToBlockChain(handshake.refundTransaction)
    } should produce [VerificationException]
  }

  it should "not be broadcastable if the timelock hasn't expired yet" in {
    val userWallet = createWallet(exchange.userKey, 2 bitcoins)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    evaluating {
      sendToBlockChain(handshake.refundTransaction)
    } should produce [VerificationException]
  }

  it should "not be broadcastable after the timelock expired if is hasn't been signed" in {
    val userWallet = createWallet(exchange.userKey, 2 bitcoins)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    (1L to exchange.lockTime).foreach(_ => mineBlock())
    evaluating {
      sendToBlockChain(handshake.refundTransaction)
    } should produce [VerificationException]
  }

  it should "be broadcastable after the timelock expired if is has been signed" in {
    val initialAmount = 3 bitcoins
    val userWallet = createWallet(exchange.userKey, initialAmount)
    val commitmentAmount = 2 bitcoins
    val handshake = new DefaultExchangeHandshake(
      exchange,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    (1L to exchange.lockTime).foreach(_ => mineBlock())
    val signatures = List(exchange.counterpartKey, exchange.userKey).map(key =>
      handshake.refundTransaction.calculateSignature(
        0,
        key,
        handshake.commitmentTransaction.getOutput(0).getScriptPubKey,
        SigHash.ALL,
        false))
    handshake.refundTransaction.getInput(0).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
    sendToBlockChain(handshake.refundTransaction)
    BtcAmount(userWallet.getBalance) should be (initialAmount)
  }

  "The happy path" should "just work!" in {
    val userWallet = createWallet(exchange.userKey, 3 bitcoins)
    val userHandshake = new DefaultExchangeHandshake(
      exchange,
      amountToCommit = 2 bitcoins,
      userWallet) {}

    val counterpartWallet = createWallet(exchange.counterpartKey, 5 bitcoins)
    val counterpartExchange = exchange.copy(
      userKey = exchange.counterpartKey,
      counterpartKey = exchange.userKey)
    val counterpartHandshake = new DefaultExchangeHandshake(
      counterpartExchange,
      3 bitcoins,
      counterpartWallet) {}

    def signRefund(exchange: Exchange, userHandshake: ExchangeHandshake, counterpartHandshake: ExchangeHandshake) {
      val signatures = List(
        throughWire(counterpartHandshake.signCounterpartRefundTransaction(
          throughWire(userHandshake.refundTransaction)).get),
        userHandshake.refundTransaction.calculateSignature(
          0,
          exchange.userKey,
          userHandshake.commitmentTransaction.getOutput(0).getScriptPubKey,
          SigHash.ALL,
          false))
      userHandshake.refundTransaction.getInput(0).setScriptSig(
        ScriptBuilder.createMultiSigInputScript(signatures))
    }

    signRefund(exchange, userHandshake, counterpartHandshake)
    signRefund(counterpartExchange, counterpartHandshake, userHandshake)

    sendToBlockChain(
      counterpartHandshake.commitmentTransaction,
      userHandshake.commitmentTransaction)


    (1L to exchange.lockTime).foreach(_ => mineBlock())
    sendToBlockChain(counterpartHandshake.refundTransaction, userHandshake.refundTransaction)
  }
}
