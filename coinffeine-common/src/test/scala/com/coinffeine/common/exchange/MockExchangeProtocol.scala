package com.coinffeine.common.exchange

import java.math.BigInteger
import scala.util.{Failure, Success, Try}

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.exchange.Exchange.Deposits
import com.coinffeine.common.exchange.MicroPaymentChannel._
import com.coinffeine.common.network.CoinffeineUnitTestNetwork

class MockExchangeProtocol extends ExchangeProtocol {

  override def createHandshake(
      exchange: OngoingExchange[FiatCurrency],
      role: Role,
      unspentOutputs: Seq[UnspentOutput],
      changeAddress: Address) = new MockHandshake(exchange, role)

  override def createMicroPaymentChannel(exchange: OngoingExchange[FiatCurrency],
                                         role: Role, deposits: Exchange.Deposits) =
    new MockMicroPaymentChannel(exchange)

  override def validateDeposits(transactions: Both[ImmutableTransaction],
                                exchange: OngoingExchange[FiatCurrency]): Try[Deposits] =
    validateCommitments(transactions, null).map(_ => Deposits(transactions))

  override def validateCommitments(transactions: Both[ImmutableTransaction],
                                   amounts: Exchange.Amounts[FiatCurrency]): Try[Unit] =
    transactions.toSeq match {
      case Seq(MockExchangeProtocol.InvalidDeposit, _) =>
        Failure(new IllegalArgumentException("Invalid buyer deposit"))
      case Seq(_, MockExchangeProtocol.InvalidDeposit) =>
        Failure(new IllegalArgumentException("Invalid seller deposit"))
      case _ => Success {}
    }
}

object MockExchangeProtocol {

  val DummyDeposit = ImmutableTransaction(new MutableTransaction(CoinffeineUnitTestNetwork))
  val DummyDeposits = Exchange.Deposits(Both(DummyDeposit, DummyDeposit))

  /** Magic deposit that is always rejected */
  val InvalidDeposit = ImmutableTransaction {
    val tx = new MutableTransaction(CoinffeineUnitTestNetwork)
    tx.setLockTime(42)
    tx
  }

  val DummySignatures = Signatures(TransactionSignature.dummy, TransactionSignature.dummy)

  /** Magic signature that is always rejected */
  val InvalidSignature = new TransactionSignature(BigInteger.valueOf(42), BigInteger.valueOf(42))
}
