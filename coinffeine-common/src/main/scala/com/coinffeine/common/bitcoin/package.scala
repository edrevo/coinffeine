package com.coinffeine.common

import java.math.BigInteger

import com.google.bitcoin.{core, crypto}

package object bitcoin {

  /** Bitcoinj's ECKey contains a public key and optionally the private one. We might replace
    * this typedef by a class ensuring that both public and private key are present.
    */
  type KeyPair = core.ECKey
  type PublicKey = core.ECKey

  type MutableTransactionOutput = core.TransactionOutput
  type MutableTransaction = core.Transaction
  object MutableTransaction {
    val ReferenceDefaultMinTxFee = core.Transaction.REFERENCE_DEFAULT_MIN_TX_FEE
  }

  type Address = core.Address
  type Hash = core.Sha256Hash
  type Network = core.NetworkParameters

  type TransactionSignature = crypto.TransactionSignature
  object TransactionSignature {

    def dummy = crypto.TransactionSignature.dummy()

    def decode(bytes: Array[Byte], requireCanonical: Boolean = true): TransactionSignature =
      crypto.TransactionSignature.decodeFromBitcoin(bytes, requireCanonical)
  }

  type Wallet = core.Wallet
  object Wallet {

    def defaultFeePerKb: BigInteger = core.Wallet.SendRequest.DEFAULT_FEE_PER_KB

    def defaultFeePerKb_= (value: BigInteger): Unit = {
      core.Wallet.SendRequest.DEFAULT_FEE_PER_KB = value
    }
  }
}
