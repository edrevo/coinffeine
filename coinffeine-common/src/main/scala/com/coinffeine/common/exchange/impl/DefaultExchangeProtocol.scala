package com.coinffeine.common.exchange.impl

import scala.util.Try

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Address, ImmutableTransaction, PublicKey}
import com.coinffeine.common.exchange._

private[impl] class DefaultExchangeProtocol extends ExchangeProtocol {

  override def createHandshake(
      exchange: OngoingExchange[FiatCurrency],
      role: Role,
      unspentOutputs: Seq[UnspentOutput],
      changeAddress: Address): Handshake = {
    val availableFunds = TransactionProcessor.valueOf(unspentOutputs.map(_.output))
    val depositAmount = role.myDepositAmount(exchange.amounts)
    require(availableFunds >= depositAmount,
      s"Expected deposit with $depositAmount ($availableFunds given)")
    val myDeposit = ImmutableTransaction {
      TransactionProcessor.createMultiSignedDeposit(
        unspentOutputs.map(_.toTuple), depositAmount, changeAddress,
        exchange.requiredSignatures, exchange.parameters.network)
    }
    new DefaultHandshake(exchange, role, myDeposit)
  }

  override def createMicroPaymentChannel(
      exchange: OngoingExchange[FiatCurrency], role: Role, deposits: Exchange.Deposits) =
    new DefaultMicroPaymentChannel(exchange, role, deposits)

  override def validateCommitments(transactions: Both[ImmutableTransaction],
                                   amounts: Exchange.Amounts[FiatCurrency]): Try[Unit] = for {
    requiredSignatures <- DepositValidator.validateRequiredSignatures(transactions)
    validator = new DepositValidator(amounts, requiredSignatures)
    _ <- validator.requireValidBuyerFunds(transactions.buyer)
    _ <- validator.requireValidSellerFunds(transactions.seller)
  } yield ()

  override def validateDeposits(transactions: Both[ImmutableTransaction],
                                exchange: OngoingExchange[FiatCurrency]) =
    new DepositValidator(exchange.amounts, exchange.requiredSignatures.toSet).validate(transactions)
}

object DefaultExchangeProtocol {
  trait Component extends ExchangeProtocol.Component {
    override lazy val exchangeProtocol = new DefaultExchangeProtocol
  }
}
