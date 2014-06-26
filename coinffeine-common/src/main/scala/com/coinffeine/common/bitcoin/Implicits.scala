package com.coinffeine.common.bitcoin

import scala.collection.JavaConversions._

import com.google.bitcoin.core.Transaction
import com.google.bitcoin.wallet.WalletTransaction

import com.coinffeine.common._
import com.coinffeine.common.Currency.Implicits._

object Implicits {

  implicit class PimpMyWallet(val wallet: Wallet) extends AnyVal {

    def value(tx: MutableTransaction): BitcoinAmount =
      Currency.Bitcoin.fromSatoshi(tx.getValue(wallet))

    def valueSentFromMe(tx: MutableTransaction): BitcoinAmount =
      Currency.Bitcoin.fromSatoshi(tx.getValueSentFromMe(wallet))

    def valueSentToMe(tx: MutableTransaction): BitcoinAmount =
      Currency.Bitcoin.fromSatoshi(tx.getValueSentToMe(wallet))

    def balance(): BitcoinAmount = Currency.Bitcoin.fromSatoshi(wallet.getBalance)

    def blockFunds(to: Address, amount: BitcoinAmount): MutableTransaction = {
      val tx = wallet.createSend(to, amount.asSatoshi)
      wallet.commitTx(tx)
      tx
    }

    def releaseFunds(tx: Transaction): Unit = {
      tx.getInputs.foreach { input =>
        val parentTx = input.getOutpoint.getConnectedOutput.getParentTransaction
        if (contains(parentTx)) {
          if (!input.disconnect()) {
            throw new IllegalStateException(s"cannot disconnect outputs from $input in $tx")
          }
          moveToPool(parentTx, WalletTransaction.Pool.UNSPENT)
        }
      }
      moveToPool(tx, WalletTransaction.Pool.DEAD)
    }

    private def contains(tx: Transaction): Boolean =
      Option(wallet.getTransaction(tx.getHash)).isDefined

    private def moveToPool(tx: Transaction, pool: WalletTransaction.Pool): Unit = {
      val wtxs = wallet.getWalletTransactions
      wallet.clearTransactions(0)
      wallet.addWalletTransaction(new WalletTransaction(pool, tx))
      wtxs.foreach { wtx =>
        if (tx.getHash != wtx.getTransaction.getHash) {
          wallet.addWalletTransaction(wtx)
        }
      }
    }
  }

}
