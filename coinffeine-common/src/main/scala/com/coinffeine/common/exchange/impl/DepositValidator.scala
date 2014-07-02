package com.coinffeine.common.exchange.impl

import scala.util.{Failure, Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.{Both, Exchange}
import com.coinffeine.common.exchange.Exchange.Deposits

private[impl] class DepositValidator(amounts: Exchange.Amounts[FiatCurrency],
                                     requiredSignatures: Both[PublicKey]) {

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
    val MultiSigInfo(possibleKeys, requiredKeyCount) = MultiSigInfo.fromScript(funds.getScriptPubKey)
      .getOrElse(throw new IllegalArgumentException(
        "Transaction with funds is invalid because is not sending the funds to a multisig"))
    require(requiredKeyCount == 2, "Funds are sent to a multisig that do not require 2 keys")
    require(possibleKeys == requiredSignatures.toSeq,
      "Possible keys in multisig script does not match the expected keys")
  }
}

private[impl] object DepositValidator {

  /** Buyer and seller deposits must share multisig keys to be coherent to each other */
  def validateRequiredSignatures(commitments: Both[ImmutableTransaction]): Try[Both[PublicKey]] =
    for {
      buyerKeys <- validateTwoRequiredKeys(commitments.buyer)
      sellerKeys <- validateTwoRequiredKeys(commitments.seller)
    } yield {
      require(buyerKeys == sellerKeys,
        s"Buyer and seller deposit keys doesn't match: $buyerKeys != $sellerKeys")
      buyerKeys
    }

  private def validateTwoRequiredKeys(transaction: ImmutableTransaction): Try[Both[PublicKey]] =
    for {
      multisigInfo <- validateMultisigTransaction(transaction)
    } yield {
      require(multisigInfo.possibleKeys.size == 2, "Number of possible keys other than 2")
      require(multisigInfo.requiredKeyCount == 2, "Number of required keys other than 2")
      Both.fromSeq(multisigInfo.possibleKeys)
    }

  private def validateMultisigTransaction(transaction: ImmutableTransaction): Try[MultiSigInfo] =
    getOrFail(MultiSigInfo.fromScript(transaction.get.getOutput(0).getScriptPubKey),
      new IllegalArgumentException(s"$transaction not in multisig"))

  private def getOrFail[T](opt: Option[T], cause: => Throwable): Try[T] =
    opt.map(Success.apply).getOrElse(Failure(cause))
}
