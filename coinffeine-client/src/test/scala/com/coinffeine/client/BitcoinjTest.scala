package com.coinffeine.client

import java.io.File
import java.math.BigInteger
import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Try

import com.google.bitcoin.core._
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.utils.BriefLogFormatter
import com.google.bitcoin.store.H2FullPrunedBlockStore
import org.scalatest.{FlatSpec, BeforeAndAfter}
import org.scalatest.matchers.ShouldMatchers

import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.Implicits._

/** Base class for testing against an in-memory, validated blockchain.  */
abstract class BitcoinjTest extends FlatSpec
  with ShouldMatchers with BeforeAndAfter with WithSampleExchangeInfo
  with CoinffeineUnitTestParams.Component {

  var blockStorePath: File = _
  var blockStore: H2FullPrunedBlockStore = _
  var chain: FullPrunedBlockChain = _

  before{
    BriefLogFormatter.init()
    Wallet.SendRequest.DEFAULT_FEE_PER_KB = BigInteger.ZERO
    createH2BlockStore()
    chain = new FullPrunedBlockChain(network, blockStore)
  }

  after {
    blockStore.close()
    destroyH2BlockStore()
    Wallet.SendRequest.DEFAULT_FEE_PER_KB = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE
  }

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

  /** Create a wallet and mine bitcoins into it until getting at least `amount` in its balance. */
  def createWallet(key: ECKey, amount: BtcAmount): Wallet = {
    val wallet = createWallet(key)
    sendMoneyToWallet(wallet, amount)
    wallet
  }

  /** Mine bitcoins into a wallet until having a minimum amount. */
  def sendMoneyToWallet(wallet: Wallet, amount: BtcAmount) {
    val miner = new ECKey
    val minerWallet = createWallet(miner)
    while (BtcAmount(minerWallet.getBalance) < amount) {
      mineBlock(miner)
    }
    sendToBlockChain(minerWallet.createSend(
      wallet.getKeys.head.toAddress(network), amount.asSatoshi))
  }

  /** Mine a block and send the coinbase reward to the passed key. */
  def mineBlock(miner: ECKey) = sendToBlockChain(miner)

  def mineBlock() = sendToBlockChain()

  /** Mine a new block with the passed transactions.
    *
    * @param miner  Destination key of the coinbase
    * @param txs    Transactions to include in the new block
    */
  def sendToBlockChain(miner: ECKey, txs: Transaction*) {
    val lastBlock = blockStore.getChainHead
    val newBlock = lastBlock.getHeader.createNextBlockWithCoinbase(
      miner.getPubKey, (50 BTC).asSatoshi)
    txs.foreach(newBlock.addTransaction)
    newBlock.solve()
    chain.add(newBlock)
  }

  /** Mine a new block with the passed transactions. */
  def sendToBlockChain(txs: Transaction*): Unit = sendToBlockChain(new ECKey(), txs: _*)

  /** Performs a serialization roundtrip to guarantee that it can be sent to a remote peer. */
  def throughWire(tx: Transaction) = new Transaction(network, tx.bitcoinSerialize())

  /** Performs a serialization roundtrip to guarantee that it can be sent to a remote peer. */
  def throughWire(sig: TransactionSignature) =
    TransactionSignature.decodeFromBitcoin(sig.encodeToBitcoin(), true)

  private def createH2BlockStore() {
    blockStorePath = File.createTempFile("temp", "blockStore")
    blockStorePath.delete()
    blockStorePath.mkdir()
    blockStore = new H2FullPrunedBlockStore(network, new File(blockStorePath, "db").toString, 1000)
    blockStore.resetStore()
  }

  private def destroyH2BlockStore() {
    blockStore.close()
    recursiveDelete(blockStorePath)
  }

  private def recursiveDelete(file: File) {
    val files = Option(file.listFiles()).getOrElse(Array.empty)
    files.foreach(recursiveDelete)
    file.delete()
  }
}
