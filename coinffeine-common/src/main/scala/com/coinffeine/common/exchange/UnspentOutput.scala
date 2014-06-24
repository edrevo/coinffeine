package com.coinffeine.common.exchange

import com.coinffeine.common.BitcoinAmount
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.impl.TransactionProcessor

/** An output not yet spent and the key to spend it.
  *
  * Outputs should be unspent and the key value is expected to have the private key to spend the
  * former.
  */
class UnspentOutput(val output: MutableTransactionOutput, val key: KeyPair) {
  def toTuple: (MutableTransactionOutput, KeyPair) = (output, key)
}

object UnspentOutput {

  /** Collect outputs greedily until reaching a minimum amount of bitcoins */
  def collect(minimumAmount: BitcoinAmount, wallet: Wallet): Seq[UnspentOutput] =
    TransactionProcessor.collectFunds(wallet, minimumAmount).toSeq.map { output =>
      val key = wallet.findKeyFromPubHash(output.getScriptPubKey.getPubKeyHash)
      new UnspentOutput(output, key)
    }
}
