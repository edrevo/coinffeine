package com.coinffeine.arbiter

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.core.Transaction
import org.scalatest.mock.MockitoSugar

import com.coinffeine.common.{PeerConnection, AkkaSpec}
import com.coinffeine.common.blockchain.BlockchainActor.PublishTransaction
import com.coinffeine.common.protocol._
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.currency.BtcAmount
import com.coinffeine.common.currency.CurrencyCode.EUR

class HandshakeArbiterActorTest
  extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("HandshakeArbiterSystem"))
  with MockitoSugar {

  class WithTestArbiter(timeout: FiniteDuration = 1 minute) {
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

    val txSerialization = new FakeTransactionSerialization(
      transactions = Seq(buyerTx, sellerTx, invalidCommitmentTx),
      signatures = Seq.empty
    )

    val listener = TestProbe()
    val gateway = TestProbe()
    val blockchain = TestProbe()
    val arbiter = system.actorOf(Props(new HandshakeArbiterActor(
      arbiter = TestCommitmentValidation,
      gateway = gateway.ref,
      blockchain = blockchain.ref,
      transactionSerialization = txSerialization,
      constants = ProtocolConstants(commitmentAbortTimeout = timeout)
    )))
    listener.watch(arbiter)

    def shouldSubscribeForMessages() = {
      listener.send(arbiter, orderMatch)
      gateway.expectMsgClass(classOf[Subscribe])
    }

    def shouldAbort(reason: String) {
      val notification = ExchangeAborted(exchangeId, reason)
      gateway.expectMsgAllOf(
        ForwardMessage(notification, buyer),
        ForwardMessage(notification, seller)
      )
      listener.expectTerminated(arbiter)
    }
  }

  "An arbiter" must "subscribe to relevant messages" in new WithTestArbiter {
    val Subscribe(filter) = shouldSubscribeForMessages()
    val commitmentTransaction = new Array[Byte](0)
    val unknownPeer = PeerConnection("unknownPeer")

    val relevantEntrance = EnterExchange(exchangeId, commitmentTransaction)
    filter(ReceiveMessage(relevantEntrance, buyer)) should be (true)
    filter(ReceiveMessage(relevantEntrance, seller)) should be (true)
    filter(ReceiveMessage(EnterExchange("other exchange", commitmentTransaction), buyer)) should be (false)
    filter(ReceiveMessage(relevantEntrance, unknownPeer)) should be (false)

    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), buyer)) should be (true)
    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), seller)) should be (true)
    filter(ReceiveMessage(ExchangeRejection("other exchange", "reason"), seller)) should be (false)
    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), unknownPeer)) should be (false)
  }

  it must "check TXs, publish them and terminate" in new WithTestArbiter {
    shouldSubscribeForMessages()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, buyerTx.bitcoinSerialize()), buyer))
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, sellerTx.bitcoinSerialize()), seller))
    blockchain.expectMsgAllOf(PublishTransaction(buyerTx), PublishTransaction(sellerTx))
    val notification = CommitmentNotification(exchangeId, buyerTx.getHash, sellerTx.getHash)
    gateway.expectMsgAllOf(
      ForwardMessage(notification, buyer),
      ForwardMessage(notification, seller)
    )
    listener.expectTerminated(arbiter)
  }

  it must "cancel exchange if a TX is not valid" in new WithTestArbiter {
    shouldSubscribeForMessages()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, invalidCommitmentTx.bitcoinSerialize()), buyer))
    shouldAbort(s"Invalid commitment from $buyer")
  }

  it must "cancel handshake when participant rejects it" in new WithTestArbiter {
    shouldSubscribeForMessages()
    gateway.expectNoMsg(500 millis)
    gateway.send(arbiter, ReceiveMessage(ExchangeRejection(exchangeId, "Got impatient"), seller))
    val notification = ExchangeAborted(exchangeId, "Rejected by counterpart: Got impatient")
    gateway.expectMsg(ForwardMessage(notification, buyer))
    listener.expectTerminated(arbiter)
  }

  it must "cancel handshake on timeout" in new WithTestArbiter(timeout = 1 second) {
    shouldSubscribeForMessages()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, buyerTx.bitcoinSerialize()), buyer))
    gateway.expectNoMsg(100 millis)
    shouldAbort("Timeout waiting for commitments")
  }
}
