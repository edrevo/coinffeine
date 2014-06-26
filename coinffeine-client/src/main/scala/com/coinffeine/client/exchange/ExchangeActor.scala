package com.coinffeine.client.exchange

import akka.actor._

import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.client.micropayment.MicroPaymentChannelActor
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.StartMicroPaymentChannel
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Hash, MutableTransaction, Wallet}
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.exchange._
import com.coinffeine.common.protocol.ProtocolConstants

class ExchangeActor[C <: FiatCurrency](
    handshakeActorProps: Props,
    microPaymentChannelActorProps: Props,
    exchangeProtocol: ExchangeProtocol,
    microPaymentChannelFactory: MicropaymentChannelFactory[C],
    constants: ProtocolConstants,
    resultListeners: Set[ActorRef]) extends Actor with ActorLogging {

  val receive: Receive = {
    case init: StartExchange[C] => new InitializedExchange(init).start()
  }

  private class InitializedExchange(init: StartExchange[C]) {
    import init._

    def start(): Unit = {
      require(userWallet.getKeys.contains(role.me(exchange).bitcoinKey))
      log.info(s"Starting exchange ${exchange.id}")
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

    private val inMicropaymentChannel: Receive = {
      case MicroPaymentChannelActor.ExchangeSuccess =>
        log.info(s"Finishing exchange '${exchange.id}' successfully")
        finishWith(ExchangeSuccess)
      case MicroPaymentChannelActor.ExchangeFailure(e, lastOffer) =>
        // TODO: handle failure with AbortActor
        log.warning(s"Finishing exchange '${exchange.id}' with a failure due to ${e.toString}")
        finishWith(ExchangeFailure(e))
    }

    private val inHandshake: Receive = {
      case HandshakeSuccess(sellerCommitmentTxId, buyerCommitmentTxId, refundTx) =>
        // TODO: next step for refundTx
        context.child(HandshakeActorName).map(context.stop)
        blockchain ! RetrieveTransaction(sellerCommitmentTxId)
        blockchain ! RetrieveTransaction(buyerCommitmentTxId)
        context.become(receiveTransaction(sellerCommitmentTxId, buyerCommitmentTxId))
      case HandshakeFailure(err) => finishWith(ExchangeFailure(err))
    }

    private def receiveTransaction(
        sellerCommitmentTxId: Hash, buyerCommitmentTxId: Hash): Receive = {
      val commitmentTxIds = Seq(sellerCommitmentTxId, buyerCommitmentTxId)
      def withReceivedTxs(receivedTxs: Map[Hash, MutableTransaction]): Receive = {
        case TransactionFound(id, tx) =>
          val newTxs = receivedTxs.updated(id, tx)
          if (commitmentTxIds.forall(newTxs.keySet.contains)) {
            val channel = microPaymentChannelFactory(
              exchange,
              role,
              paymentProcessor,
              newTxs(sellerCommitmentTxId),
              newTxs(buyerCommitmentTxId))
            val microPaymentChannelActorRef = context.actorOf(
              microPaymentChannelActorProps, MicroPaymentChannelActorName)
            microPaymentChannelActorRef ! StartMicroPaymentChannel[C](
              exchange, role, channel, constants, paymentProcessor, messageGateway, resultListeners = Set(self)
            )
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
      if (context == null) println("null context")
      if (self == null) println("null self")
      context.stop(self)
    }

    private def watchForCounterpartDeposit(): Unit = {
      blockchain ! WatchPublicKey(role.her(exchange).bitcoinKey)
    }
  }
}

object ExchangeActor {

  val HandshakeActorName = "handshake"
  val MicroPaymentChannelActorName = "exchange"

  type MicropaymentChannelFactory[C <: FiatCurrency] = (
    Exchange[C],
    Role,
    ActorRef,
    MutableTransaction, // sellerCommitmentTx
    MutableTransaction // buyerCommitmentTx
  ) => ProtoMicroPaymentChannel[C]

  case class StartExchange[C <: FiatCurrency](
    exchange: Exchange[C],
    role: Role,
    userWallet: Wallet,
    paymentProcessor: ActorRef,
    messageGateway: ActorRef,
    blockchain: ActorRef
  )

  case object ExchangeSuccess

  case class ExchangeFailure(e: Throwable)

  case class CommitmentTxNotInBlockChain(txId: Hash) extends RuntimeException(
    s"Handshake reported that the commitment transaction with hash $txId was in " +
      s"blockchain but it could not be found")
}
