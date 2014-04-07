package com.coinffeine.arbiter

import scala.concurrent.duration._

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.core.Transaction
import org.scalatest.mock.MockitoSugar

import com.coinffeine.arbiter.HandshakeArbiterActor.StartHandshake
import com.coinffeine.common.{AkkaSpec, PeerConnection}
import com.coinffeine.common.blockchain.BlockchainActor.PublishTransaction
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR
import com.coinffeine.common.network.CoinffeineUnitTestParams
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.arbitration.CommitmentNotification
import com.coinffeine.common.protocol.messages.brokerage.OrderMatch
import com.coinffeine.common.protocol.messages.handshake.{EnterExchange, ExchangeAborted, ExchangeRejection}

class HandshakeArbiterActorTest
  extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("HandshakeArbiterSystem"))
  with MockitoSugar {

  class WithTestArbiter(timeout: FiniteDuration = 1.minute) {
    val exchangeId: String = "1234"
    val buyer: PeerConnection = PeerConnection("buyer")
    val seller: PeerConnection = PeerConnection("seller")
    val buyerTx = MockTransaction()
    val sellerTx = MockTransaction()
    val invalidCommitmentTx = MockTransaction()
    val orderMatch = OrderMatch(exchangeId, BtcAmount(1), EUR(500), buyer, seller)

    object TestCommitmentValidation extends CommitmentValidation {
      override def isValidCommitment(committer: PeerConnection, tx: Transaction): Boolean =
        Set(buyer -> buyerTx, seller -> sellerTx).contains(committer -> tx)
    }

    val listener = TestProbe()
    val gateway = TestProbe()
    val blockchain = TestProbe()
    val arbiter = system.actorOf(Props(new HandshakeArbiterActor(
      arbiter = TestCommitmentValidation,
      constants = ProtocolConstants(commitmentAbortTimeout = timeout)
    )))
    listener.watch(arbiter)

    def shouldSubscribeForMessages() = {
      listener.send(arbiter, StartHandshake(orderMatch, gateway.ref, blockchain.ref))
      gateway.expectMsgClass(classOf[Subscribe])
    }

    def shouldAbort(reason: String): Unit = {
      val notification = ExchangeAborted(exchangeId, reason)
      gateway.expectMsgAllOf(
        ForwardMessage(notification, buyer),
        ForwardMessage(notification, seller)
      )
      listener.expectTerminated(arbiter)
    }

    def shouldNotifyOrderMatch(): Unit = {
      shouldSubscribeForMessages()
      gateway.expectMsgAllOf(ForwardMessage(orderMatch, buyer), ForwardMessage(orderMatch, seller))
    }
  }

  "An arbiter" must "subscribe to relevant messages" in new WithTestArbiter {
    val Subscribe(filter) = shouldSubscribeForMessages()
    val commitmentTransaction = new Transaction(CoinffeineUnitTestParams)
    val unknownPeer = PeerConnection("unknownPeer")

    val relevantEntrance = EnterExchange(exchangeId, commitmentTransaction)
    filter(ReceiveMessage(relevantEntrance, buyer)) should be (true)
    filter(ReceiveMessage(relevantEntrance, seller)) should be (true)
    filter(ReceiveMessage(EnterExchange("other exchange", commitmentTransaction), buyer)) should
      be (false)
    filter(ReceiveMessage(relevantEntrance, unknownPeer)) should be (false)

    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), buyer)) should be (true)
    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), seller)) should be (true)
    filter(ReceiveMessage(ExchangeRejection("other exchange", "reason"), seller)) should be (false)
    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), unknownPeer)) should be (false)
  }

  it must "notify the match, collect TXs, publish them and terminate" in new WithTestArbiter {
    shouldNotifyOrderMatch()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, buyerTx), buyer))
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, sellerTx), seller))
    blockchain.expectMsgAllOf(PublishTransaction(buyerTx), PublishTransaction(sellerTx))
    val notification = CommitmentNotification(exchangeId, buyerTx.getHash, sellerTx.getHash)
    gateway.expectMsgAllOf(
      ForwardMessage(notification, buyer),
      ForwardMessage(notification, seller)
    )
    listener.expectTerminated(arbiter)
  }

  it must "cancel exchange if a TX is not valid" in new WithTestArbiter {
    shouldNotifyOrderMatch()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, invalidCommitmentTx), buyer))
    shouldAbort(s"Invalid commitment from $buyer")
  }

  it must "cancel handshake when participant rejects it" in new WithTestArbiter {
    shouldNotifyOrderMatch()
    gateway.expectNoMsg(500 millis)
    gateway.send(arbiter, ReceiveMessage(ExchangeRejection(exchangeId, "Got impatient"), seller))
    val notification = ExchangeAborted(exchangeId, "Rejected by counterpart: Got impatient")
    gateway.expectMsg(ForwardMessage(notification, buyer))
    listener.expectTerminated(arbiter)
  }

  it must "cancel handshake on timeout" in new WithTestArbiter(timeout = 1 second) {
    shouldNotifyOrderMatch()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, buyerTx), buyer))
    gateway.expectNoMsg(100 millis)
    shouldAbort("Timeout waiting for commitments")
  }
}
