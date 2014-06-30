package com.coinffeine.common.exchange.impl

import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Both, Exchange}

import scala.util.Try

private case class ValidDeposits(override val transactions: Both[ImmutableTransaction])
  extends Exchange.Deposits

private[impl] object ValidDeposits {

  def validate(transactions: Both[ImmutableTransaction],
               exchange: Exchange[_ <: FiatCurrency]): Try[Exchange.Deposits] = Try {

    val requiredSignatures = Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey)

    def requireValidBuyerFunds(buyerFunds: MutableTransactionOutput): Unit = {
      requireValidFunds(buyerFunds)
      require(Bitcoin.fromSatoshi(buyerFunds.getValue) == exchange.amounts.stepBitcoinAmount * 2,
        "The amount of committed funds by the buyer does not match the expected amount")
    }

    def requireValidSellerFunds(sellerFunds: MutableTransactionOutput): Unit = {
      requireValidFunds(sellerFunds)
      require(
        Bitcoin.fromSatoshi(sellerFunds.getValue) ==
          exchange.parameters.bitcoinAmount + exchange.amounts.stepBitcoinAmount,
        "The amount of committed funds by the seller does not match the expected amount")
    }

    def requireValidFunds(funds: MutableTransactionOutput): Unit = {
      require(funds.getScriptPubKey.isSentToMultiSig,
        "Transaction with funds is invalid because is not sending the funds to a multisig")
      val multisigInfo = MultiSigInfo(funds.getScriptPubKey)
      require(multisigInfo.requiredKeyCount == 2,
        "Funds are sent to a multisig that do not require 2 keys")
      require(multisigInfo.possibleKeys == requiredSignatures.toSet,
        "Possible keys in multisig script does not match the expected keys")
    }

    requireValidBuyerFunds(transactions.buyer.get.getOutput(0))
    requireValidSellerFunds(transactions.seller.get.getOutput(0))

    ValidDeposits(transactions)
  }
}
