package com.coinffeine.client.handshake

import scala.collection.JavaConversions._

import com.google.bitcoin.core.VerificationException
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.{SampleExchangeInfo, ExchangeInfo}
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._

class DefaultHandshakeTest extends BitcoinjTest with SampleExchangeInfo {
  val exchangeInfo = sampleExchangeInfo

  "The DefaultExchangeHandshake constructor" should
    "fail if the wallet does not contain the private key" in {
      an [IllegalArgumentException] should be thrownBy {
        new DefaultHandshake(
          exchangeInfo = exchangeInfo,
          amountToCommit = 2 BTC,
          userWallet = createWallet()) {}
      }
    }

  "The refund transaction" should "not be directly broadcastable to the blockchain" in {
    val userWallet = createWallet(exchangeInfo.userKey, 2 BTC)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    a [VerificationException] should be thrownBy sendToBlockChain(handshake.refundTransaction)
  }

  it should "not be broadcastable if the timelock hasn't expired yet" in {
    val userWallet = createWallet(exchangeInfo.userKey, 2 BTC)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    a [VerificationException] should be thrownBy sendToBlockChain(handshake.refundTransaction)
  }

  it should "not be broadcastable after the timelock expired if is hasn't been signed" in {
    val userWallet = createWallet(exchangeInfo.userKey, 2 BTC)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    (1L to exchangeInfo.parameters.lockTime).foreach(_ => mineBlock())
    a [VerificationException] should be thrownBy sendToBlockChain(handshake.refundTransaction)
  }

  it should "be broadcastable after the timelock expired if is has been signed" in {
    val initialAmount = 3 BTC
    val userWallet = createWallet(exchangeInfo.userKey, initialAmount)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction)
    (1L to exchangeInfo.parameters.lockTime).foreach(_ => mineBlock())
    val signatures = List(exchangeInfo.counterpartKey, exchangeInfo.userKey).map(key =>
      handshake.refundTransaction.calculateSignature(
        0,
        key,
        handshake.commitmentTransaction.getOutput(0).getScriptPubKey,
        SigHash.ALL,
        false))
    handshake.refundTransaction.getInput(0)
      .setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
    sendToBlockChain(handshake.refundTransaction)
    Currency.Bitcoin.fromSatoshi(userWallet.getBalance) should be (initialAmount)
  }

  "The happy path" should "just work!" in {
    val userWallet = createWallet(exchangeInfo.userKey, 3 BTC)
    val userHandshake = new DefaultHandshake(
      exchangeInfo,
      amountToCommit = 2 BTC,
      userWallet) {}

    val counterpartWallet = createWallet(exchangeInfo.counterpartKey, 5 BTC)
    val counterpartExchange: ExchangeInfo[Currency.Euro.type] = exchangeInfo.copy(
      userKey = exchangeInfo.counterpartKey,
      counterpartKey = exchangeInfo.userKey)
    val counterpartHandshake = new DefaultHandshake(
      counterpartExchange,
      3 BTC,
      counterpartWallet) {}

    def signRefund(
        exchangeInfo: ExchangeInfo[Currency.Euro.type],
        userHandshake: Handshake[Currency.Euro.type],
        counterpartHandshake: Handshake[Currency.Euro.type]): Unit = {
      val signatures = List(
        throughWire(counterpartHandshake.signCounterpartRefundTransaction(
          throughWire(userHandshake.refundTransaction)).get),
        userHandshake.refundTransaction.calculateSignature(
          0,
          exchangeInfo.userKey,
          userHandshake.commitmentTransaction.getOutput(0).getScriptPubKey,
          SigHash.ALL,
          false))
      userHandshake.refundTransaction.getInput(0).setScriptSig(
        ScriptBuilder.createMultiSigInputScript(signatures))
    }

    signRefund(exchangeInfo, userHandshake, counterpartHandshake)
    signRefund(counterpartExchange, counterpartHandshake, userHandshake)

    sendToBlockChain(
      counterpartHandshake.commitmentTransaction,
      userHandshake.commitmentTransaction)

    for (_ <- 1L to exchangeInfo.parameters.lockTime) { mineBlock() }
    sendToBlockChain(counterpartHandshake.refundTransaction, userHandshake.refundTransaction)
  }
}
