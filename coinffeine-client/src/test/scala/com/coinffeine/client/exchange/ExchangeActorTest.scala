package com.coinffeine.client.exchange

import java.util.concurrent.LinkedBlockingDeque
import scala.concurrent.duration._

import akka.actor.{ActorRef, Props, Terminated}
import akka.testkit.TestActor.Message
import akka.testkit.{TestActor, TestProbe}
import akka.util.Timeout
import com.google.bitcoin.core.{Sha256Hash, Transaction, Wallet}
import com.google.bitcoin.crypto.TransactionSignature
import org.scalatest.concurrent.Eventually

import com.coinffeine.client.exchange.ExchangeActor._
import com.coinffeine.client.handshake.HandshakeActor.{HandshakeFailure, HandshakeSuccess, StartHandshake}
import com.coinffeine.client.handshake.{Handshake, MockHandshake}
import com.coinffeine.client.{CoinffeineClientTest, ExchangeInfo}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor
import com.coinffeine.client.paymentprocessor.MockPaymentProcessorFactory
import com.coinffeine.common.Currency.Euro
import com.coinffeine.common.PeerConnection
import com.coinffeine.common.blockchain.BlockchainActor.{GetTransactionFor, TransactionFor, TransactionNotFoundWith}
import com.coinffeine.common.network.UnitTestNetworkComponent
import com.coinffeine.common.protocol.ProtocolConstants


class ExchangeActorTest extends CoinffeineClientTest("buyerExchange")
  with UnitTestNetworkComponent with Eventually {

  implicit def testTimeout = new Timeout(5 second)
  private val exchangeInfo = sampleExchangeInfo
  private val protocolConstants = ProtocolConstants(
    commitmentConfirmations = 1,
    resubmitRefundSignatureTimeout = 1 second,
    refundSignatureAbortTimeout = 1 minute)

  private val handshake = new MockHandshake(exchangeInfo)
  private def handshakeFactory(
      exchangeInfo: ExchangeInfo[Euro.type], wallet: Wallet): Handshake[Euro.type] = handshake
  private val handshakeActorMessageQueue = new LinkedBlockingDeque[Message]()
  private val handshakeProps = TestActor.props(handshakeActorMessageQueue)

  private val exchange = new MockExchange(exchangeInfo) with BuyerUser[Euro.type]
  private def exchangeFactory(
      exchangeInfo: ExchangeInfo[Euro.type],
      paymentProc: ActorRef,
      tx1: Transaction,
      tx2: Transaction): Exchange[Euro.type] with BuyerUser[Euro.type] = exchange
  private val exchangeActorMessageQueue = new LinkedBlockingDeque[Message]()
  private val exchangeProps = TestActor.props(exchangeActorMessageQueue)

  override val broker: PeerConnection = exchangeInfo.broker
  override val counterpart: PeerConnection = exchangeInfo.counterpart

  private val dummySig = TransactionSignature.dummy
  private val dummyTxId = new Sha256Hash(List.fill(64)("F").mkString)
  private val dummyTx = new Transaction(network)
  private val userWallet = {
    val wallet = new Wallet(network)
    wallet.addKey(exchangeInfo.userKey)
    wallet
  }
  private val dummyPaymentProcessor = system.actorOf(new MockPaymentProcessorFactory(List.empty).newProcessor(
    fiatAddress = "", initialBalance = Seq.empty))

  trait Fixture {
    val listener = TestProbe()
    val blockchain = TestProbe()
    val actor = system.actorOf(
      Props(new ExchangeActor[Euro.type, BuyerUser[Euro.type]](
        handshakeProps,
        exchangeProps,
        handshakeFactory,
        exchangeFactory,
        protocolConstants,
        Set(listener.ref))))
    listener.watch(actor)

    def withActor(actorName: String)(body: ActorRef => Unit) = {
      val actorSelection = system.actorSelection(actor.path / actorName)
      eventually {
        actorSelection.resolveOne().futureValue
      }
      whenReady(actorSelection.resolveOne())(body)
    }
  }

  "The exchange actor" should "report an exchange success if both handshake " +
      "and exchange work as expected" in new Fixture {
    actor ! StartExchange(
      exchangeInfo, userWallet, dummyPaymentProcessor, gateway.ref, blockchain.ref)
    withActor(HandshakeActorName) { handshakeActor =>
      val queueItem = handshakeActorMessageQueue.pop()
      queueItem.msg should be (StartHandshake(
        handshake, protocolConstants, gateway.ref, blockchain.ref, Set(actor)))
      queueItem.sender should be (actor)
      actor.!(HandshakeSuccess(dummyTxId, dummyTxId, dummySig))(handshakeActor)
    }
    blockchain.expectMsg(GetTransactionFor(dummyTxId))
    blockchain.expectMsg(GetTransactionFor(dummyTxId))
    blockchain.reply(TransactionFor(dummyTxId, dummyTx))
    blockchain.reply(TransactionFor(dummyTxId, dummyTx))

    withActor(MicroPaymentChannelActorName) { exchangeActor =>
      val queueItem = exchangeActorMessageQueue.pop()
      queueItem.msg should be (MicroPaymentChannelActor.StartMicroPaymentChannel(
        exchange, protocolConstants, gateway.ref, Set(actor)))
      queueItem.sender should be (actor)
      actor.!(MicroPaymentChannelActor.ExchangeSuccess)(exchangeActor)
    }
    listener.expectMsg(ExchangeSuccess)
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the handshake fails" in new Fixture {
    actor ! StartExchange(
      exchangeInfo, userWallet, dummyPaymentProcessor, gateway.ref, blockchain.ref)
    val error = new Error("Handshake error")
    withActor(HandshakeActorName) { handshakeActor =>
      val queueItem = handshakeActorMessageQueue.pop()
      queueItem.msg should be (StartHandshake(
        handshake, protocolConstants, gateway.ref, blockchain.ref, Set(actor)))
      queueItem.sender should be (actor)
      actor.!(HandshakeFailure(error))(handshakeActor)
    }
    listener.expectMsg(ExchangeFailure(error))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the blockchain can't find the commitment txs" in new Fixture {
    actor ! StartExchange(
      exchangeInfo, userWallet, dummyPaymentProcessor, gateway.ref, blockchain.ref)
    withActor(HandshakeActorName) { handshakeActor =>
      val queueItem = handshakeActorMessageQueue.pop()
      queueItem.msg should be (StartHandshake(
        handshake, protocolConstants, gateway.ref, blockchain.ref, Set(actor)))
      queueItem.sender should be (actor)
      actor.!(HandshakeSuccess(dummyTxId, dummyTxId, dummySig))(handshakeActor)
    }
    blockchain.expectMsg(GetTransactionFor(dummyTxId))
    blockchain.expectMsg(GetTransactionFor(dummyTxId))
    blockchain.reply(TransactionNotFoundWith(dummyTxId))
    blockchain.reply(TransactionFor(dummyTxId, dummyTx))

    val error = new CommitmentTxNotInBlockChain(dummyTxId)
    listener.expectMsg(ExchangeFailure(error))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }

  it should "report a failure if the actual exchange fails" in new Fixture {
    actor ! StartExchange(
      exchangeInfo, userWallet, dummyPaymentProcessor, gateway.ref, blockchain.ref)
    withActor(HandshakeActorName) { handshakeActor =>
      val queueItem = handshakeActorMessageQueue.pop()
      queueItem.msg should be (StartHandshake(
        handshake, protocolConstants, gateway.ref, blockchain.ref, Set(actor)))
      queueItem.sender should be (actor)
      actor.!(HandshakeSuccess(dummyTxId, dummyTxId, dummySig))(handshakeActor)
    }
    blockchain.expectMsg(GetTransactionFor(dummyTxId))
    blockchain.expectMsg(GetTransactionFor(dummyTxId))
    blockchain.reply(TransactionFor(dummyTxId, dummyTx))
    blockchain.reply(TransactionFor(dummyTxId, dummyTx))

    val error = new Error("exchange failure")
    withActor(MicroPaymentChannelActorName) { exchangeActor =>
      val queueItem = exchangeActorMessageQueue.pop()
      queueItem.msg should be (MicroPaymentChannelActor.StartMicroPaymentChannel(
        exchange, protocolConstants, gateway.ref, Set(actor)))
      queueItem.sender should be (actor)
      actor.!(MicroPaymentChannelActor.ExchangeFailure(error, None))(exchangeActor)
    }
    listener.expectMsg(ExchangeFailure(error))
    listener.expectMsgClass(classOf[Terminated])
    system.stop(actor)
  }
}
