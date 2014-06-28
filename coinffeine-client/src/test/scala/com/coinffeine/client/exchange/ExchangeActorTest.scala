package com.coinffeine.client.exchange

import scala.concurrent.duration._

import akka.actor.{ActorRef, Props, Terminated}
import akka.testkit.{TestActor, TestProbe}
import akka.util.Timeout
import org.scalatest.concurrent.Eventually

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.SellerPerspective
import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.client.exchange.ExchangeTransactionBroadcastActor._
import com.coinffeine.client.handshake.MockExchangeProtocol
import com.coinffeine.client.handshake.HandshakeActor.{HandshakeFailure, HandshakeSuccess, StartHandshake}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.bitcoin._
import com.coinffeine.common.bitcoin.peers.PeerActor.{TransactionPublished, BlockchainActorReference, RetrieveBlockchainActor}
import com.coinffeine.common.blockchain.BlockchainActor._
import com.coinffeine.common.exchange.Both
import com.coinffeine.common.protocol.ProtocolConstants

class ExchangeActorTest extends CoinffeineClientTest("buyerExchange")
  with SellerPerspective with BitcoinjTest with Eventually {

  implicit def testTimeout = new Timeout(5 second)
  private val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)

  private val handshakeActorMessageQueue = new TestMessageQueue()
  private val micropaymentChannelActorMessageQueue = new TestMessageQueue()
  private val transactionBroadcastActorMessageQueue = new TestMessageQueue()

  private val deposits = Both(
    buyer = new Hash(List.fill(64)("0").mkString),
    seller = new Hash(List.fill(64)("1").mkString)
  )
  private val dummyTx = ImmutableTransaction(new MutableTransaction(network))
  private val dummyPaymentProcessor = system.actorOf(
    new MockPaymentProcessorFactory(List.empty)
      .newProcessor(fiatAddress = "", initialBalance = Seq.empty)
  )

  trait Fixture {
    val listener = TestProbe()
    val blockchain = TestProbe()
    val peers = TestProbe()
    val wallet = createWallet(user.bitcoinKey, exchange.amounts.sellerDeposit)
    val handshakeProps = TestActor.props(handshakeActorMessageQueue.queue)
    val micropaymentChannelProps = TestActor.props(micropaymentChannelActorMessageQueue.queue)
    val transactionBroadcastActorProps = TestActor.props(transactionBroadcastActorMessageQueue.queue)
    val actor = system.actorOf(Props(new ExchangeActor[Euro.type](
      handshakeProps,
      micropaymentChannelProps,
      transactionBroadcastActorProps,
      new MockExchangeProtocol,
      protocolConstants,
      Set(listener.ref)
    )))
    listener.watch(actor)

    def withActor(actorName: String)(body: ActorRef => Unit) = {
      val actorSelection = system.actorSelection(actor.path / actorName)
      eventually {
        actorSelection.resolveOne().futureValue
      }
      whenReady(actorSelection.resolveOne())(body)
    }

    def startExchange(): Unit = {
      actor ! StartExchange(
        exchange, userRole, wallet, dummyPaymentProcessor, gateway.ref, peers.ref
      )
      peers.expectMsg(RetrieveBlockchainActor)
      peers.reply(BlockchainActorReference(blockchain.ref))
    }

    def givenHandshakeSuccess(): Unit = {
      withActor(HandshakeActorName) { handshakeActor =>
        handshakeActorMessageQueue.expectMsgClass[StartHandshake[_]]()
        actor.tell(HandshakeSuccess(deposits, dummyTx), handshakeActor)
      }
      transactionBroadcastActorMessageQueue.expectMsg(SetRefund(dummyTx))
    }

    def givenTransactionsAreFound(): Unit = {
      shouldWatchForTheTransactions()
      givenTransactionIsFound(deposits.buyer)
      givenTransactionIsFound(deposits.seller)
      withActor(MicroPaymentChannelActorName) { micropaymentChannelActor =>
        transactionBroadcastActorMessageQueue.expectMsg(
          SetMicropaymentActor(micropaymentChannelActor))
      }
    }

    def givenTransactionIsFound(txId: Hash): Unit = {
      blockchain.reply(TransactionFound(txId, dummyTx))
    }

    def givenTransactionIsNotFound(txId: Hash): Unit = {
      blockchain.reply(TransactionNotFound(txId))
    }

    def givenTransactionIsCorrectlyBroadcast(): Unit =
      withActor(TransactionBroadcastActorName) { txBroadcaster =>
        transactionBroadcastActorMessageQueue.expectMsg(FinishExchange)
        actor.tell(ExchangeFinished(TransactionPublished(dummyTx, dummyTx)), txBroadcaster)
      }

    def givenMicropaymentChannelSuccess(): Unit =
      withActor(MicroPaymentChannelActorName) { micropaymentChannelActor =>
        micropaymentChannelActorMessageQueue.expectMsg(MicroPaymentChannelActor.StartMicroPaymentChannel(
          exchange, userRole, MockExchangeProtocol.DummyDeposits, protocolConstants,
          dummyPaymentProcessor, gateway.ref, Set(actor)
        ))
        actor.tell(MicroPaymentChannelActor.ExchangeSuccess(Some(dummyTx)), micropaymentChannelActor)
      }

    def shouldWatchForTheTransactions(): Unit = {
      blockchain.expectMsg(WatchPublicKey(counterpart.bitcoinKey))
      blockchain.expectMsgAllOf(
        RetrieveTransaction(deposits.buyer),
        RetrieveTransaction(deposits.seller)
      )
    }
  }

  "The exchange actor" should "report an exchange success when handshake, exchange and broadcast work" in
    new Fixture {
      startExchange()
      givenHandshakeSuccess()
      givenTransactionsAreFound()
      givenMicropaymentChannelSuccess()
      givenTransactionIsCorrectlyBroadcast()
      listener.expectMsg(ExchangeSuccess)
      listener.expectMsgClass(classOf[Terminated])
      system.stop(actor)
    }

  it should "report a failure if the handshake fails" in new Fixture {
    startExchange()
    val error = new Error("Handshake error")
    withActor(HandshakeActorName) { handshakeActor =>
      handshakeActorMessageQueue.expectMsgClass[StartHandshake[_]]()
      actor.tell(HandshakeFailure(error), handshakeActor)
    }
    listener.expectMsg(ExchangeFailure(error))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the blockchain can't find the commitment txs" in new Fixture {
    startExchange()
    givenHandshakeSuccess()
    shouldWatchForTheTransactions()
    givenTransactionIsFound(deposits.buyer)
    givenTransactionIsNotFound(deposits.seller)

    listener.expectMsg(ExchangeFailure(new CommitmentTxNotInBlockChain(deposits.seller)))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the actual exchange fails" in new Fixture {
    startExchange()
    givenHandshakeSuccess()
    givenTransactionsAreFound()

    val error = new Error("exchange failure")
    withActor(MicroPaymentChannelActorName) { micropaymentChannelActor =>
      micropaymentChannelActorMessageQueue.expectMsg(MicroPaymentChannelActor.StartMicroPaymentChannel(
        exchange, userRole, MockExchangeProtocol.DummyDeposits, protocolConstants,
        dummyPaymentProcessor, gateway.ref, Set(actor)
      ))
      actor.tell(MicroPaymentChannelActor.ExchangeFailure(error), micropaymentChannelActor)
    }

    givenTransactionIsCorrectlyBroadcast()

    listener.expectMsg(ExchangeFailure(error))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the broadcast failed" in new Fixture {
    startExchange()
    givenHandshakeSuccess()
    givenTransactionsAreFound()
    givenMicropaymentChannelSuccess()
    val broadcastError = new Error("failed to broadcast")
    withActor(TransactionBroadcastActorName) { txBroadcaster =>
      transactionBroadcastActorMessageQueue.expectMsg(FinishExchange)
      actor.tell(ExchangeFinishFailure(broadcastError), txBroadcaster)
    }
    listener.expectMsg(ExchangeFailure(TxBroadcastFailed(broadcastError)))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the broadcast succeeds with an unexpected transaction" in new Fixture {
    startExchange()
    givenHandshakeSuccess()
    givenTransactionsAreFound()
    givenMicropaymentChannelSuccess()
    val unexpectedTx = ImmutableTransaction {
      val newTx = dummyTx.get
      newTx.setLockTime(40)
      newTx
    }
    withActor(TransactionBroadcastActorName) { txBroadcaster =>
      transactionBroadcastActorMessageQueue.expectMsg(FinishExchange)
      actor.tell(ExchangeFinished(TransactionPublished(unexpectedTx, unexpectedTx)), txBroadcaster)
    }
    listener.expectMsg(ExchangeFailure(UnexpectedTxBroadcast(unexpectedTx, dummyTx)))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the broadcast is forcefully finished because it took too long" in new Fixture {
    startExchange()
    givenHandshakeSuccess()
    givenTransactionsAreFound()
    val midWayTx = ImmutableTransaction {
      val newTx = dummyTx.get
      newTx.setLockTime(40)
      newTx
    }
    withActor(TransactionBroadcastActorName) { txBroadcaster =>
      transactionBroadcastActorMessageQueue.queue should be ('empty)
      actor.tell(ExchangeFinished(TransactionPublished(midWayTx, midWayTx)), txBroadcaster)
    }
    listener.expectMsg(ExchangeFailure(RiskOfValidRefund(midWayTx)))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }
}
