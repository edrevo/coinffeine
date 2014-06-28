package com.coinffeine.client.exchange

import akka.actor._

import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.client.handshake.HandshakeActor._
import com.coinffeine.client.micropayment.MicroPaymentChannelActor
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.StartMicroPaymentChannel
import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.bitcoin.{Hash, ImmutableTransaction, MutableTransaction, Wallet}
import com.coinffeine.common.bitcoin.peers.PeerActor.{BlockchainActorReference, RetrieveBlockchainActor}
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.exchange._
import com.coinffeine.common.protocol.ProtocolConstants

class ExchangeActor[C <: FiatCurrency](
    handshakeActorProps: Props,
    microPaymentChannelActorProps: Props,
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

    private val inMicropaymentChannel: Receive = {
      case MicroPaymentChannelActor.ExchangeSuccess =>
        log.info(s"Finishing exchange '${exchange.id}' successfully")
        finishWith(ExchangeSuccess)
      case MicroPaymentChannelActor.ExchangeFailure(e, lastOffer) =>
        // TODO: handle failure with AbortActor
        log.warning(s"Finishing exchange '${exchange.id}' with a failure due to ${e.toString}")
        finishWith(ExchangeFailure(e))
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
  }
}

object ExchangeActor {

  val HandshakeActorName = "handshake"
  val MicroPaymentChannelActorName = "exchange"

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
}
