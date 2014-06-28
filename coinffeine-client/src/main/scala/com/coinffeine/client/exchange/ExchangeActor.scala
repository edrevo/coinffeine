package com.coinffeine.client.exchange

import akka.actor._

import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.client.exchange.ExchangeTransactionBroadcastActor._
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.client.micropayment.MicroPaymentChannelActor
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.StartMicroPaymentChannel
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Hash, ImmutableTransaction, MutableTransaction, Wallet}
import com.coinffeine.common.bitcoin.peers.PeerActor.{TransactionPublished, BlockchainActorReference, RetrieveBlockchainActor}
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.exchange._
import com.coinffeine.common.protocol.ProtocolConstants

class ExchangeActor[C <: FiatCurrency](
    handshakeActorProps: Props,
    microPaymentChannelActorProps: Props,
    transactionBroadcastActorProps: Props,
    exchangeProtocol: ExchangeProtocol,
    constants: ProtocolConstants,
    resultListeners: Set[ActorRef]) extends Actor with ActorLogging {

  val receive: Receive = {
    case init: StartExchange[C] => new InitializedExchange(init).start()
  }

  private class InitializedExchange(init: StartExchange[C]) {
    import init._
    var blockchain: ActorRef = _

    def start(): Unit = {
      require(userWallet.getKeys.contains(role.me(exchange).bitcoinKey))
      log.info(s"Starting exchange ${exchange.id}")
      bitcoinPeers ! RetrieveBlockchainActor
      context.become(retrievingBlockchain)
    }

    private val inHandshake: Receive = {
      case HandshakeSuccess(commitmentTxIds, refundTx) =>
        context.child(HandshakeActorName).map(context.stop)
        val txBroadcaster = context.actorOf(
          transactionBroadcastActorProps, TransactionBroadcastActorName)
        txBroadcaster ! SetRefund(refundTx)
        commitmentTxIds.toSeq.foreach(id => blockchain ! RetrieveTransaction(id))
        context.become(receiveTransaction(commitmentTxIds))
      case HandshakeFailure(err) => finishWith(ExchangeFailure(err))
    }

    private val retrievingBlockchain: Receive = {
      case BlockchainActorReference(blockchainRef) =>
        blockchain = blockchainRef
        watchForCounterpartDeposit()
        startHandshake()
        context.become(inHandshake)
    }

    private def startHandshake(): Unit = {
      // TODO: ask the wallet actor for funds
      val funds = UnspentOutput.collect(role.myDepositAmount(exchange.amounts), userWallet)
      val handshake =
        exchangeProtocol.createHandshake(exchange, role, funds, userWallet.getChangeAddress)
      context.actorOf(handshakeActorProps, HandshakeActorName) ! StartHandshake(
        exchange, role, handshake, constants, messageGateway, blockchain, resultListeners = Set(self)
      )
    }

    private def finishingExchange(
        result: Any, expectedFinishingTx: Option[ImmutableTransaction]): Receive = {
      case ExchangeFinished(TransactionPublished(originalTx, broadcastTx))
          if expectedFinishingTx.exists(_ != originalTx) =>
        val err = UnexpectedTxBroadcast(originalTx, expectedFinishingTx.get)
        log.error(err, "The transaction broadcast for this exchange is different from the one " +
          "that was being expected.")
        log.error("The previous exchange result is going to be overridden by this unexpected error.")
        log.error(s"Previous result: $result")
        finishWith(ExchangeFailure(err))
      case ExchangeFinished(_) =>
        finishWith(result)
      case ExchangeFinishFailure(err) =>
        log.error(err, "The finishing transaction could not be broadcast")
        log.error("The previous exchange result is going to be overridden by this unexpected error.")
        log.error(s"Previous result: $result")
        finishWith(ExchangeFailure(TxBroadcastFailed(err)))
    }

    private val inMicropaymentChannel: Receive = {
      case MicroPaymentChannelActor.ExchangeSuccess(successTx) =>
        log.info(s"Finishing exchange '${exchange.id}' successfully")
        txBroadcaster ! FinishExchange
        context.become(finishingExchange(ExchangeSuccess, successTx))
      case MicroPaymentChannelActor.ExchangeFailure(e) =>
        log.warning(s"Finishing exchange '${exchange.id}' with a failure due to ${e.toString}")
        txBroadcaster ! FinishExchange
        context.become(finishingExchange(ExchangeFailure(e), None))
      case ExchangeFinished(TransactionPublished(_, broadcastTx)) =>
        finishWith(ExchangeFailure(RiskOfValidRefund(broadcastTx)))
    }

    private def receiveTransaction(commitmentTxIds: Both[Hash]): Receive = {
      def withReceivedTxs(receivedTxs: Map[Hash, ImmutableTransaction]): Receive = {
        case TransactionFound(id, tx) =>
          val newTxs = receivedTxs.updated(id, tx)
          if (commitmentTxIds.toSeq.forall(newTxs.keySet.contains)) {
            // TODO: what if counterpart deposit is not valid?
            val deposits = exchangeProtocol.validateDeposits(commitmentTxIds.map(newTxs), exchange).get
            val ref = context.actorOf(microPaymentChannelActorProps, MicroPaymentChannelActorName)
            ref ! StartMicroPaymentChannel[C](
              exchange, role, deposits, constants, paymentProcessor, messageGateway, resultListeners = Set(self)
            )
            txBroadcaster ! SetMicropaymentActor(ref)
            context.become(inMicropaymentChannel)
          } else {
            context.become(withReceivedTxs(newTxs))
          }
        case TransactionNotFound(txId) =>
          finishWith(ExchangeFailure(CommitmentTxNotInBlockChain(txId)))
      }
      withReceivedTxs(Map.empty)
    }

    private def finishWith(result: Any): Unit = {
      resultListeners.foreach { _ ! result }
      context.stop(self)
    }

    private def watchForCounterpartDeposit(): Unit = {
      // TODO for the PR: why are we doing this?
      blockchain ! WatchPublicKey(role.her(exchange).bitcoinKey)
    }

    private def txBroadcaster = context.child(TransactionBroadcastActorName).getOrElse {
      val message = "Transaction broadcast actor does not exist"
      log.error(message)
      throw new Error(message)
    }
  }
}

object ExchangeActor {

  val HandshakeActorName = "handshake"
  val MicroPaymentChannelActorName = "exchange"
  val TransactionBroadcastActorName = "transactionBroadcast"

  case class StartExchange[C <: FiatCurrency](
    exchange: Exchange[C],
    role: Role,
    userWallet: Wallet,
    paymentProcessor: ActorRef,
    messageGateway: ActorRef,
    bitcoinPeers: ActorRef
  )

  case object ExchangeSuccess

  case class ExchangeFailure(e: Throwable)

  case class CommitmentTxNotInBlockChain(txId: Hash) extends RuntimeException(
    s"Handshake reported that the commitment transaction with hash $txId was in " +
      s"blockchain but it could not be found")

  case class UnexpectedTxBroadcast(effectiveTx: ImmutableTransaction, expectedTx: ImmutableTransaction)
    extends RuntimeException(
      s"""The transaction broadcast for this exchange is different from the one that was being expected.
            |   Sent transaction: $effectiveTx
            |   Expected: $expectedTx""".stripMargin)

  case class TxBroadcastFailed(cause: Throwable) extends RuntimeException(
    "The final transaction could not be broadcast", cause)

  case class RiskOfValidRefund(broadcastTx: ImmutableTransaction) extends RuntimeException(
    "The exchange was forcefully finished because it was taking too long and there was a chance" +
      "that the refund transaction could have become valid"
  )
}
