package com.coinffeine.common

import java.io.File
import java.math.BigInteger
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.util.Try

import com.google.bitcoin.core.{FullPrunedBlockChain, StoredBlock}
import com.google.bitcoin.store.H2FullPrunedBlockStore
import com.google.bitcoin.utils.BriefLogFormatter
import org.scalatest.BeforeAndAfterAll

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.network.CoinffeineUnitTestNetwork

/** Base class for testing against an in-memory, validated blockchain.  */
trait BitcoinjTest extends UnitTest with CoinffeineUnitTestNetwork.Component with BeforeAndAfterAll {

  var blockStorePath: File = _
  var blockStore: H2FullPrunedBlockStore = _
  var chain: FullPrunedBlockChain = _

  if (resetBlockchainBetweenTests) {
    before { startBitcoinj() }
    after { stopBitcoinj() }
  }

  override def beforeAll(): Unit = {
    if (!resetBlockchainBetweenTests) {
      startBitcoinj()
    }
  }
  override def afterAll(): Unit = {
    if (!resetBlockchainBetweenTests) {
      stopBitcoinj()
    }
  }

  def chainHead(): StoredBlock = chain.getChainHead

  def withFees[A](body: => A) = {
    Wallet.defaultFeePerKb = MutableTransaction.ReferenceDefaultMinTxFee.asSatoshi
    val result = Try(body)
    Wallet.defaultFeePerKb = BigInteger.ZERO
    result.get
  }

  def createWallet(): Wallet = {
    val wallet = new Wallet(network)
    chain.addWallet(wallet)
    wallet
  }

  def createWallet(key: KeyPair): Wallet = {
    val wallet = createWallet()
    wallet.addKey(key)
    wallet
  }

  /** Create a wallet and mine bitcoins into it until getting at least `amount` in its balance. */
  def createWallet(key: KeyPair, amount: BitcoinAmount): Wallet = {
    val wallet = createWallet(key)
    sendMoneyToWallet(wallet, amount)
    wallet
  }

  /** Mine bitcoins into a wallet until having a minimum amount. */
  def sendMoneyToWallet(wallet: Wallet, amount: BitcoinAmount): Unit = {
    val miner = new KeyPair
    val minerWallet = createWallet(miner)
    while (Currency.Bitcoin.fromSatoshi(minerWallet.getBalance) < amount) {
      mineBlock(miner)
    }
    sendToBlockChain(minerWallet.createSend(
      wallet.getKeys.head.toAddress(network), amount.asSatoshi))
  }

  /** Mine a block and send the coinbase reward to the passed key. */
  def mineBlock(miner: PublicKey) = sendToBlockChain(miner)

  def mineBlock() = sendToBlockChain()

  def mineUntilLockTime(lockTime: Long): Unit = {
    while (blockStore.getChainHead.getHeight < lockTime) {
      mineBlock()
    }
  }

  /** Mine a new block with the passed transactions using the given last block.
    *
    * @param lastBlock The last block to be considered the chain head
    * @param miner     Destination key of the coinbase
    * @param txs       Transactions to include in the new block
    * @return          The new blockchain header
    */
  def sendToBlockChain(lastBlock: StoredBlock,
                       miner: PublicKey,
                       txs: MutableTransaction*): StoredBlock = {
    @tailrec
    def retrySend(remainingAttempts: Int): StoredBlock = {
      if (remainingAttempts < 0) {
        throw new IllegalStateException(
          "after several attempts, cannot send the given transactions to the blockchain")
      }
      val newBlock = lastBlock.getHeader.createNextBlockWithCoinbase(
        miner.getPubKey, 50.BTC.asSatoshi)
      txs.foreach(newBlock.addTransaction)
      newBlock.solve()
      if (!chain.add(newBlock)) {
        Thread.sleep(250)
        retrySend(remainingAttempts - 1)
      }
      else chain.getBlockStore.get(newBlock.getHash)
    }
    retrySend(3)
  }

  /** Mine a new block with the passed transactions.
    *
    * @param miner  Destination key of the coinbase
    * @param txs    Transactions to include in the new block
    */
  def sendToBlockChain(miner: PublicKey, txs: MutableTransaction*): StoredBlock = {
    sendToBlockChain(blockStore.getChainHead, miner, txs: _*)
  }

  /** Mine a new block with the passed transactions. */
  def sendToBlockChain(txs: MutableTransaction*): StoredBlock =
    sendToBlockChain(new PublicKey(), txs: _*)

  /** Performs a serialization roundtrip to guarantee that it can be sent to a remote peer. */
  def throughWire(tx: MutableTransaction) = new MutableTransaction(network, tx.bitcoinSerialize())

  /** Performs a serialization roundtrip to guarantee that it can be sent to a remote peer. */
  def throughWire(sig: TransactionSignature) = TransactionSignature.decode(sig.encodeToBitcoin())

  /** Most test classes require blockchain isolation between tests. Tests building a step-by-step
    * history should override this function. */
  protected def resetBlockchainBetweenTests: Boolean = true

  private def startBitcoinj(): Unit = {
    BitcoinjTest.ExecutionLock.lock()
    BriefLogFormatter.init()
    Wallet.defaultFeePerKb = BigInteger.ZERO
    createH2BlockStore()
    chain = new FullPrunedBlockChain(network, blockStore)
  }

  private def createH2BlockStore(): Unit = {
    blockStorePath = File.createTempFile("temp", "blockStore")
    blockStorePath.delete()
    blockStorePath.mkdir()
    blockStore = new H2FullPrunedBlockStore(network, new File(blockStorePath, "db").toString, 1000)
    blockStore.resetStore()
  }

  private def stopBitcoinj(): Unit = {
    try {
      blockStore.close()
      destroyH2BlockStore()
      Wallet.defaultFeePerKb = MutableTransaction.ReferenceDefaultMinTxFee.asSatoshi
    } finally {
      BitcoinjTest.ExecutionLock.unlock()
    }
  }

  private def destroyH2BlockStore(): Unit = {
    blockStore.close()
    recursiveDelete(blockStorePath)
  }

  private def recursiveDelete(file: File): Unit = {
    val files = Option(file.listFiles()).getOrElse(Array.empty)
    files.foreach(recursiveDelete)
    file.delete()
  }
}

private object BitcoinjTest {
  /** Bitcoinj uses global state such as the TX fees than cannot be changed in isolation so we
    * need to serialize test executions. */
  val ExecutionLock: Lock = new ReentrantLock()
}
