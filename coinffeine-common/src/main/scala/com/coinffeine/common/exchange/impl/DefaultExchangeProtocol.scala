package com.coinffeine.common.exchange.impl

import scala.util.Try

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Address, ImmutableTransaction}
import com.coinffeine.common.exchange._

private[impl] class DefaultExchangeProtocol extends ExchangeProtocol {

  override def createHandshake(
      exchange: Exchange[_ <: FiatCurrency],
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
        Seq(exchange.buyer.bitcoinKey, exchange.seller.bitcoinKey), exchange.parameters.network)
    }
    new DefaultHandshake(exchange, role, myDeposit)
  }

  override def createMicroPaymentChannel(
      exchange: Exchange[_  <: FiatCurrency], role: Role, deposits: Exchange.Deposits) =
    new DefaultMicroPaymentChannel(role, exchange, deposits)

  override def validateDeposit(role: Role, transaction: ImmutableTransaction,
                               exchange: Exchange[_ <: FiatCurrency]): Try[Unit] = {
    val validator = new DepositValidator(exchange)
    role match {
      case BuyerRole => validator.requireValidBuyerFunds(transaction)
      case SellerRole => validator.requireValidSellerFunds(transaction)
    }
  }

  override def validateDeposits(transactions: Both[ImmutableTransaction],
                                exchange: Exchange[_ <: FiatCurrency]) =
    new DepositValidator(exchange).validate(transactions)
}

object DefaultExchangeProtocol {
  trait Component extends ExchangeProtocol.Component {
    override lazy val exchangeProtocol = new DefaultExchangeProtocol
  }
}
