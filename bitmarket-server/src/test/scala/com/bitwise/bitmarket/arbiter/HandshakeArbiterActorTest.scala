package com.bitwise.bitmarket.arbiter

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Props
import akka.testkit.TestProbe
import com.google.bitcoin.core.{Transaction, Sha256Hash}
import org.mockito.BDDMockito.given
import org.scalatest.mock.MockitoSugar

import com.bitwise.bitmarket.common.{PeerConnection, AkkaSpec}
import com.bitwise.bitmarket.common.blockchain.BlockchainActor.PublishTransaction
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.gateway.MessageGateway._
import com.bitwise.bitmarket.common.currency.BtcAmount
import com.bitwise.bitmarket.common.currency.CurrencyCode.EUR

class HandshakeArbiterActorTest
  extends AkkaSpec(AkkaSpec.systemWithLoggingInterception("HandshakeArbiterSystem"))
  with MockitoSugar {

  class WithTestArbiter(timeout: FiniteDuration = 1 minute) {
    val exchangeId: String = "1234"
    val buyer: PeerConnection = PeerConnection("buyer")
    val seller: PeerConnection = PeerConnection("seller")
    val buyerTx = mock[Transaction]
    val buyerTxId = mock[Sha256Hash]
    given(buyerTx.getHash).willReturn(buyerTxId)
    val sellerTx = mock[Transaction]
    val sellerTxId = mock[Sha256Hash]
    given(sellerTx.getHash).willReturn(sellerTxId)
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
      gateway = gateway.ref,
      blockchain = blockchain.ref,
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
    val unknownPeer = PeerConnection("unknownPeer")

    val relevantEntrance = EnterExchange(exchangeId, mock[Transaction])
    filter(ReceiveMessage(relevantEntrance, buyer)) should be (true)
    filter(ReceiveMessage(relevantEntrance, seller)) should be (true)
    filter(ReceiveMessage(EnterExchange("other exchange", mock[Transaction]), buyer)) should be (false)
    filter(ReceiveMessage(relevantEntrance, unknownPeer)) should be (false)

    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), buyer)) should be (true)
    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), seller)) should be (true)
    filter(ReceiveMessage(ExchangeRejection("other exchange", "reason"), seller)) should be (false)
    filter(ReceiveMessage(ExchangeRejection(exchangeId, "reason"), unknownPeer)) should be (false)
  }

  it must "check TXs, publish them and terminate" in new WithTestArbiter {
    shouldSubscribeForMessages()
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, buyerTx), buyer))
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, sellerTx), seller))
    blockchain.expectMsgAllOf(PublishTransaction(buyerTx), PublishTransaction(sellerTx))
    val notification = CommitmentNotification(exchangeId, buyerTxId, sellerTxId)
    gateway.expectMsgAllOf(
      ForwardMessage(notification, buyer),
      ForwardMessage(notification, seller)
    )
    listener.expectTerminated(arbiter)
  }

  it must "cancel exchange if a TX is not valid" in new WithTestArbiter {
    shouldSubscribeForMessages()
    val invalidTransaction = mock[Transaction]
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, invalidTransaction), buyer))
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
    gateway.send(arbiter, ReceiveMessage(EnterExchange(exchangeId, buyerTx), buyer))
    gateway.expectNoMsg(100 millis)
    shouldAbort("Timeout waiting for commitments")
  }
}
