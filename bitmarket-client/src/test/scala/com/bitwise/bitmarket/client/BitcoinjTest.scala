package com.bitwise.bitmarket.client

import java.math.BigInteger
import scala.collection.JavaConversions._
import scala.util.Try

import com.google.bitcoin.core.{AbstractBlockChain, ECKey, Transaction, Wallet}
import com.google.bitcoin.utils.{TestUtils, TestWithWallet}
import com.google.bitcoin.utils.TestUtils._
import org.scalatest.{FlatSpec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import com.bitwise.bitmarket.common.currency.BtcAmount

abstract class BitcoinjTest extends TestWithWallet with FlatSpec with BeforeAndAfter with ShouldMatchers {
  import TestWithWallet._
  before{
    super.setUp()
  }

  after {
    super.tearDown()
  }

  val network = params

  def withFees[A](body: => A) = {
    Wallet.SendRequest.DEFAULT_FEE_PER_KB = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE
    val result = Try(body)
    Wallet.SendRequest.DEFAULT_FEE_PER_KB = BigInteger.ZERO
    result.get
  }

  def createWallet(): Wallet = {
    val wallet = new Wallet(network)
    chain.addWallet(wallet)
    wallet
  }

  def createWallet(key: ECKey): Wallet = {
    val wallet = createWallet()
    wallet.addKey(key)
    wallet
  }

  def createWallet(key: ECKey, amount: BtcAmount): Wallet = {
    val wallet = createWallet(key)
    sendMoneyToWallet(wallet, amount)
    wallet
  }

  def sendMoneyToWallet(wallet: Wallet, amount: BtcAmount): Transaction = sendMoneyToWallet(
    wallet,
    amount.asSatoshi,
    wallet.getKeys.head.toAddress(network),
    AbstractBlockChain.NewBlockType.BEST_CHAIN)

  def sendToBlockChain(txs: Transaction*): BlockPair =
    TestUtils.createFakeBlock(blockStore, txs: _*)
}
