package com.coinffeine.client.handshake

import scala.collection.JavaConversions._

import com.google.bitcoin.core.VerificationException
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.{ExchangeInfo, SampleExchangeInfo}
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.MutableTransaction

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
    val userWallet = createWallet(exchangeInfo.user.bitcoinKey, 2 BTC)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    a [VerificationException] should be thrownBy sendToBlockChain(handshake.unsignedRefundTransaction.get)
  }

  it should "not be broadcastable if the timelock hasn't expired yet" in {
    val userWallet = createWallet(exchangeInfo.user.bitcoinKey, 2 BTC)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction.get)
    a [VerificationException] should be thrownBy sendToBlockChain(handshake.unsignedRefundTransaction.get)
  }

  it should "not be broadcastable after the timelock expired if is hasn't been signed" in {
    val userWallet = createWallet(exchangeInfo.user.bitcoinKey, 2 BTC)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction.get)
    mineUntilLockTime(exchangeInfo.parameters.lockTime)
    a [VerificationException] should be thrownBy sendToBlockChain(handshake.unsignedRefundTransaction.get)
  }

  it should "be broadcastable after the timelock expired if is has been signed" in {
    val initialAmount = 3 BTC
    val userWallet = createWallet(exchangeInfo.user.bitcoinKey, initialAmount)
    val commitmentAmount = 2 BTC
    val handshake = new DefaultHandshake(
      exchangeInfo,
      commitmentAmount,
      userWallet) {}
    sendToBlockChain(handshake.commitmentTransaction.get)
    mineUntilLockTime(exchangeInfo.parameters.lockTime)
    val tx = handshake.unsignedRefundTransaction.get
    val signatures = List(exchangeInfo.counterpart.bitcoinKey, exchangeInfo.user.bitcoinKey).map(key =>
      tx.calculateSignature(
        0,
        key,
        handshake.commitmentTransaction.get.getOutput(0).getScriptPubKey,
        SigHash.ALL,
        false))
    tx.getInput(0).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
    sendToBlockChain(tx)
    Currency.Bitcoin.fromSatoshi(userWallet.getBalance) should be (initialAmount)
  }

  "The happy path" should "just work!" in {
    val userWallet = createWallet(exchangeInfo.user.bitcoinKey, 3 BTC)
    val userHandshake = new DefaultHandshake(
      exchangeInfo,
      amountToCommit = 2 BTC,
      userWallet) {}

    val counterpartWallet = createWallet(exchangeInfo.counterpart.bitcoinKey, 5 BTC)
    val counterpartExchangeInfo: ExchangeInfo[Currency.Euro.type] = buyerExchangeInfo
    val counterpartHandshake = new DefaultHandshake(
      counterpartExchangeInfo,
      3 BTC,
      counterpartWallet) {}

    def signedRefund(exchangeInfo: ExchangeInfo[Currency.Euro.type],
                     handshake: Handshake[Currency.Euro.type],
                     counterpartHandshake: Handshake[Currency.Euro.type]): MutableTransaction = {
      val tx = handshake.unsignedRefundTransaction.get
      val signatures = List(
        throughWire(counterpartHandshake.signCounterpartRefundTransaction(throughWire(tx)).get),
        tx.calculateSignature(
          0,
          exchangeInfo.user.bitcoinKey,
          handshake.commitmentTransaction.get.getOutput(0).getScriptPubKey,
          SigHash.ALL,
          false))
      tx.getInput(0).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
      tx
    }

    sendToBlockChain(
      counterpartHandshake.commitmentTransaction.get,
      userHandshake.commitmentTransaction.get
    )
    mineUntilLockTime(exchangeInfo.parameters.lockTime)
    sendToBlockChain(
      signedRefund(exchangeInfo, userHandshake, counterpartHandshake),
      signedRefund(counterpartExchangeInfo, counterpartHandshake, userHandshake)
    )
  }
}
