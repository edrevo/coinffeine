package com.coinffeine.client

import scala.language.postfixOps

import akka.actor._
import com.google.bitcoin.core.{Sha256Hash, Transaction, Wallet}

import com.coinffeine.client.ExchangeSupervisorActor._
import com.coinffeine.client.exchange.{ExchangeActor, Exchange, UserRole}
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.client.handshake.Handshake
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.blockchain.BlockchainActor.{TransactionNotFoundWith, TransactionFor, GetTransactionFor}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.paymentprocessor.PaymentProcessor

class ExchangeSupervisorActor[C <: FiatCurrency, R <: UserRole](
    handshakeActorProps: Props,
    exchangeActorProps: Props,
    handshakeFactory: HandshakeFactory[C],
    exchangeFactory: ExchangeFactory[C, R],
    constants: ProtocolConstants,
    resultListeners: Set[ActorRef]) extends Actor with ActorLogging {

  val receive: Receive = {
    case init: StartExchange[C] => new InitializedExchangeSupervisorActor(init).start()
  }

  private class InitializedExchangeSupervisorActor(init: StartExchange[C]) {
    import init._

    def start(): Unit = {
      require(userWallet.getKeys.contains(exchangeInfo.userKey))
      log.info(s"Starting exchange ${exchangeInfo.id}")

      val handshake = handshakeFactory(exchangeInfo, userWallet)
      context.actorOf(handshakeActorProps, HandshakeActorName) ! StartHandshake(
        handshake, constants, messageGateway, blockchain, resultListeners = Set(self))
      context.become(inHandshake)
    }

    private val inExchange: Receive = {
      case ExchangeActor.ExchangeSuccess =>
        log.info(s"Finishing exchange '${exchangeInfo.id}' successfully")
        finishWith(ExchangeSuccess)
      case ExchangeActor.ExchangeFailure(e, lastOffer) =>
        // TODO: handle failure with AbortActor
        log.warning(s"Finishing exchange '${exchangeInfo.id}' with a failure due to ${e.toString}")
        finishWith(ExchangeFailure(e))
    }

    private val inHandshake: Receive = {
      case HandshakeSuccess(sellerCommitmentTxId, buyerCommitmentTxId, refundTxSig) =>
        context.child(HandshakeActorName).map(context.stop)
        blockchain ! GetTransactionFor(sellerCommitmentTxId)
        blockchain ! GetTransactionFor(buyerCommitmentTxId)
        context.become(receiveTransaction(sellerCommitmentTxId, buyerCommitmentTxId))
      case HandshakeFailure(err) => finishWith(ExchangeFailure(err))
    }

    private def receiveTransaction(
        sellerCommitmentTxId: Sha256Hash, buyerCommitmentTxId: Sha256Hash): Receive = {
      val commitmentTxIds = Seq(sellerCommitmentTxId, buyerCommitmentTxId)
      def withReceivedTxs(receivedTxs: Map[Sha256Hash, Transaction]): Receive = {
        case TransactionFor(id, tx) =>
          val newTxs = receivedTxs.updated(id, tx)
          if (commitmentTxIds.forall(newTxs.keySet.contains)) {
            val exchange = exchangeFactory(
              exchangeInfo,
              paymentProcessor,
              newTxs(sellerCommitmentTxId),
              newTxs(buyerCommitmentTxId))
            context.actorOf(exchangeActorProps, ExchangeActorName) ! ExchangeActor.StartExchange[C, R](
              exchange, constants, messageGateway, resultListeners = Set(self))
            context.become(inExchange)
          } else {
            context.become(withReceivedTxs(newTxs))
          }
        case TransactionNotFoundWith(txId) =>
          finishWith(ExchangeFailure(CommitmentTxNotInBlockChain(txId)))
      }
      withReceivedTxs(Map.empty)
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      if (context == null) println("null context")
      if (self == null) println("null self")
      context.stop(self)
    }
  }
}

object ExchangeSupervisorActor {

  val HandshakeActorName = "handshake"
  val ExchangeActorName = "exchange"

  type HandshakeFactory[C <: FiatCurrency] = (ExchangeInfo[C], Wallet) => Handshake[C]
  type ExchangeFactory[C <: FiatCurrency, Role <: UserRole] = (
    ExchangeInfo[C],
    PaymentProcessor,
    Transaction, // sellerCommitmentTx
    Transaction // buyerCommitmentTx
  ) => Exchange[C] with Role

  case class StartExchange[C <: FiatCurrency](
    exchangeInfo: ExchangeInfo[C],
    userWallet: Wallet,
    paymentProcessor: PaymentProcessor,
    messageGateway: ActorRef,
    blockchain: ActorRef
  )

  case object ExchangeSuccess

  case class ExchangeFailure(e: Throwable)

  case class CommitmentTxNotInBlockChain(txId: Sha256Hash) extends RuntimeException(
    s"Handshake reported that the commitment transaction with hash $txId was in " +
      s"blockchain but it could not be found")
}
