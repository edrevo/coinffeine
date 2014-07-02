package com.coinffeine.common.exchange.impl

import scala.util.{Failure, Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.exchange.Exchange.Deposits

private[impl] class DepositValidator(amounts: Exchange.Amounts[FiatCurrency],
                                     requiredSignatures: Set[PublicKey]) {

  def validate(transactions: Both[ImmutableTransaction]): Try[Exchange.Deposits] = for {
    _ <- requireValidBuyerFunds(transactions.buyer)
    _ <- requireValidSellerFunds(transactions.seller)
  } yield Deposits(transactions)

  def requireValidBuyerFunds(transaction: ImmutableTransaction): Try[Unit] = Try {
    val buyerFunds = transaction.get.getOutput(0)
    requireValidFunds(buyerFunds)
    require(Bitcoin.fromSatoshi(buyerFunds.getValue) == amounts.stepBitcoinAmount * 2,
      "The amount of committed funds by the buyer does not match the expected amount")
  }

  def requireValidSellerFunds(transaction: ImmutableTransaction): Try[Unit] = Try {
    val sellerFunds = transaction.get.getOutput(0)
    require(
      Bitcoin.fromSatoshi(sellerFunds.getValue) == amounts.bitcoinAmount + amounts.stepBitcoinAmount,
      "The amount of committed funds by the seller does not match the expected amount")
  }

  private def requireValidFunds(funds: MutableTransactionOutput): Unit = {
    require(funds.getScriptPubKey.isSentToMultiSig,
      "Transaction with funds is invalid because is not sending the funds to a multisig")
    val multisigInfo = MultiSigInfo(funds.getScriptPubKey)
    require(multisigInfo.requiredKeyCount == 2,
      "Funds are sent to a multisig that do not require 2 keys")
    require(multisigInfo.possibleKeys == requiredSignatures,
      "Possible keys in multisig script does not match the expected keys")
  }
}

private[impl] object DepositValidator {

  /** Buyer and seller deposits must share multisig keys to be coherent to each other */
  def validateRequiredSignatures(commitments: Both[ImmutableTransaction]): Try[Set[PublicKey]] = {
    val bothRequiredKeys = commitments.map { tx =>
      MultiSigInfo(tx.get.getOutput(0).getScriptPubKey).possibleKeys
    }
    if (bothRequiredKeys.buyer == bothRequiredKeys.seller) Success(bothRequiredKeys.buyer)
    else Failure(new IllegalArgumentException(
      s"Buyer and seller deposit keys doesn't match: $bothRequiredKeys"
    ))
  }
}
