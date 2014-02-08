package com.bitwise.bitmarket.client.handshake

import scala.language.postfixOps

import com.bitwise.bitmarket.client.{BitcoinjTest, Exchange}
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.BtcAmount.Implicits._

import com.google.bitcoin.core.ECKey
import com.google.bitcoin.core.AbstractBlockChain.NewBlockType

class DefaultExchangeHandshakeTest extends BitcoinjTest {
  val exchange = Exchange(
    id = "dummy",
    counterpart = PeerConnection("localhost", 1234),
    broker = PeerConnection("localhost", 1235),
    network = network,
    userKey = new ECKey(),
    counterpartKey = new ECKey(),
    exchangeAmount = 10 bitcoins,
    steps = 10,
    lockTime = 1567)

  "The DefaultExchangeHandshake constructor" should "fail if the wallet does not contain the private key" in {
    evaluating {
      new DefaultExchangeHandshake(
        exchange = exchange,
        amountToCommit = 2 bitcoins,
        userWallet = createWallet()) {}
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
    val blockPair = sendToBlockChain(handshake.commitmentTransaction)
    userWallet.receiveFromBlock(
      handshake.commitmentTransaction,
      blockPair.storedBlock,
      NewBlockType.BEST_CHAIN,
      0)
    BtcAmount(userWallet.getBalance) should be (0 bitcoins)
  }
}
