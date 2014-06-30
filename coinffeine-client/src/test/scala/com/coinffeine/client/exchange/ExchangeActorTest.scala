package com.coinffeine.client.exchange

import scala.concurrent.duration._

import akka.actor.{ActorRef, Props, Terminated}
import akka.testkit.{TestActor, TestProbe}
import akka.util.Timeout
import org.scalatest.concurrent.Eventually

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.CoinffeineClientTest.SellerPerspective
import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.client.handshake.MockExchangeProtocol
import com.coinffeine.client.handshake.HandshakeActor.{HandshakeFailure, HandshakeSuccess, StartHandshake}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.BitcoinjTest
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.bitcoin._
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

  private val exchangeActorMessageQueue = new TestMessageQueue()

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
    val wallet = createWallet(user.bitcoinKey, exchange.amounts.sellerDeposit)
    val handshakeProps = TestActor.props(handshakeActorMessageQueue.queue)
    val exchangeProps = TestActor.props(exchangeActorMessageQueue.queue)
    val actor = system.actorOf(Props(new ExchangeActor[Euro.type](
      handshakeProps,
      exchangeProps,
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
        exchange, userRole, wallet, dummyPaymentProcessor, gateway.ref, blockchain.ref
      )
    }

    def givenHandshakeSuccess(): Unit = withActor(HandshakeActorName) { handshakeActor =>
      handshakeActorMessageQueue.expectMsgClass[StartHandshake[_]]()
      actor.tell(HandshakeSuccess(deposits, dummyTx), handshakeActor)
    }

    def givenTransactionsAreFound(): Unit = {
      shouldWatchForTheTransactions()
      givenTransactionIsFound(deposits.buyer)
      givenTransactionIsFound(deposits.seller)
    }

    def givenTransactionIsFound(txId: Hash): Unit = {
      blockchain.reply(TransactionFound(txId, dummyTx))
    }

    def givenTransactionIsNotFound(txId: Hash): Unit = {
      blockchain.reply(TransactionNotFound(txId))
    }

    def shouldWatchForTheTransactions(): Unit = {
      blockchain.expectMsg(WatchPublicKey(counterpart.bitcoinKey))
      blockchain.expectMsgAllOf(
        RetrieveTransaction(deposits.buyer),
        RetrieveTransaction(deposits.seller)
      )
    }
  }

  "The exchange actor" should """report an exchange success when handshake and exchange work""" in
    new Fixture {
      startExchange()
      givenHandshakeSuccess()
      givenTransactionsAreFound()

      withActor(MicroPaymentChannelActorName) { exchangeActor =>
        exchangeActorMessageQueue.expectMsg(MicroPaymentChannelActor.StartMicroPaymentChannel(
          exchange, userRole, MockExchangeProtocol.DummyDeposits, protocolConstants,
          dummyPaymentProcessor, gateway.ref, Set(actor)
        ))
        actor.tell(MicroPaymentChannelActor.ExchangeSuccess, exchangeActor)
      }
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
    withActor(MicroPaymentChannelActorName) { exchangeActor =>
      exchangeActorMessageQueue.expectMsg(MicroPaymentChannelActor.StartMicroPaymentChannel(
        exchange, userRole, MockExchangeProtocol.DummyDeposits, protocolConstants,
        dummyPaymentProcessor, gateway.ref, Set(actor)
      ))
      actor.!(MicroPaymentChannelActor.ExchangeFailure(error, None))(exchangeActor)
    }

    listener.expectMsg(ExchangeFailure(error))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }
}
