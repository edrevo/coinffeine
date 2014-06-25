package com.coinffeine.client.handshake

import com.coinffeine.common
import com.coinffeine.common.exchange.impl.DefaultExchangeProtocol

import scala.collection.JavaConversions._

import com.google.bitcoin.core.VerificationException
import com.google.bitcoin.core.Transaction.SigHash
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.{ExchangeInfo, SampleExchangeInfo}
import com.coinffeine.common.{BitcoinjTest, Currency}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.MutableTransaction
import com.coinffeine.common.exchange.UnspentOutput

class DefaultHandshakeTest
  extends BitcoinjTest with SampleExchangeInfo with DefaultExchangeProtocol.Component {

  val exchangeInfo = sampleExchangeInfo

  "The refund transaction" should "not be directly broadcastable to the blockchain" in
    new UserFixture {
      a [VerificationException] should be thrownBy sendToBlockChain(userHandshake.myUnsignedRefund.get)
    }

  it should "not be broadcastable if the timelock hasn't expired yet" in new UserFixture {
    sendToBlockChain(userHandshake.myDeposit.get)
    a [VerificationException] should be thrownBy sendToBlockChain(userHandshake.myUnsignedRefund.get)
  }

  it should "not be broadcastable after the timelock expired if is hasn't been signed" in
    new UserFixture {
      sendToBlockChain(userHandshake.myDeposit.get)
      mineUntilLockTime(exchangeInfo.parameters.lockTime)
      a [VerificationException] should be thrownBy sendToBlockChain(userHandshake.myUnsignedRefund.get)
    }

  it should "be broadcastable after the timelock expired if is has been signed" in
    new UserFixture {
      sendToBlockChain(userHandshake.myDeposit.get)
      mineUntilLockTime(exchangeInfo.parameters.lockTime)
      val tx = userHandshake.myUnsignedRefund.get
      val signatures = List(exchangeInfo.counterpart.bitcoinKey, exchangeInfo.user.bitcoinKey).map(key =>
        tx.calculateSignature(
          0,
          key,
          userHandshake.myDeposit.get.getOutput(0).getScriptPubKey,
          SigHash.ALL,
          false))
      tx.getInput(0).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
      sendToBlockChain(tx)
      Currency.Bitcoin.fromSatoshi(userWallet.getBalance) should be (initialAmount - 1.BTC)
    }

  "The happy path" should "just work!" in new UserFixture with CounterpartFixture {

    def signedRefund(exchangeInfo: ExchangeInfo[Currency.Euro.type],
                     handshake: common.exchange.Handshake[Currency.Euro.type],
                     counterpartHandshake: common.exchange.Handshake[Currency.Euro.type]): MutableTransaction = {
      val tx = handshake.myUnsignedRefund.get
      val signatures = List(
        throughWire(counterpartHandshake.signHerRefund(handshake.myUnsignedRefund)),
        tx.calculateSignature(
          0,
          exchangeInfo.user.bitcoinKey,
          handshake.myDeposit.get.getOutput(0).getScriptPubKey,
          SigHash.ALL,
          false))
      tx.getInput(0).setScriptSig(ScriptBuilder.createMultiSigInputScript(signatures))
      tx
    }

    sendToBlockChain(
      counterpartHandshake.myDeposit.get,
      userHandshake.myDeposit.get
    )
    mineUntilLockTime(exchangeInfo.parameters.lockTime)
    sendToBlockChain(
      userHandshake.signMyRefund(counterpartHandshake.signHerRefund(userHandshake.myUnsignedRefund)).get,
      counterpartHandshake.signMyRefund(userHandshake.signHerRefund(counterpartHandshake.myUnsignedRefund)).get
    )
  }

  trait UserFixture {
    val initialAmount = 30.BTC
    val userWallet = createWallet(exchangeInfo.user.bitcoinKey, initialAmount)
    val userHandshake = exchangeProtocol.createHandshake(
      exchange, exchangeInfo.role, UnspentOutput.collect(11.BTC, userWallet),
      userWallet.getChangeAddress
    )
  }

  trait CounterpartFixture {
    val counterpartWallet = createWallet(exchangeInfo.counterpart.bitcoinKey, 5.BTC)
    val counterpartExchangeInfo: ExchangeInfo[Currency.Euro.type] = buyerExchangeInfo
    val counterpartHandshake = exchangeProtocol.createHandshake(
      exchange, counterpartExchangeInfo.role, UnspentOutput.collect(2.BTC, counterpartWallet),
      counterpartWallet.getChangeAddress
    )
  }
}
