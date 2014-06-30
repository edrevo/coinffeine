package com.coinffeine.client.exchange

import akka.actor.{ActorRef, Props}
import akka.testkit.TestProbe
import com.google.bitcoin.core.TransactionInput
import com.google.bitcoin.crypto.TransactionSignature
import com.google.bitcoin.script.ScriptBuilder

import com.coinffeine.client.CoinffeineClientTest
import com.coinffeine.client.exchange.ExchangeTransactionBroadcastActor.{SetMicropaymentActor, FinishExchange, ExchangeFinished, StartBroadcastHandling}
import com.coinffeine.client.micropayment.MicroPaymentChannelActor.{LastOffer, GetLastOffer}
import com.coinffeine.common.bitcoin.peers.PeerActor.{TransactionPublished, PublishTransaction, BlockchainActorReference, RetrieveBlockchainActor}
import com.coinffeine.common.bitcoin.{MutableTransaction, ImmutableTransaction}
import com.coinffeine.common.blockchain.BlockchainActor.{BlockchainHeightReached, WatchBlockchainHeight}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.system

class ExchangeTransactionBroadcastActorTest extends CoinffeineClientTest("txBroadcastTest") {

  private val refundLockTime = 20
  private val refundTx = ImmutableTransaction {
    val tx = new MutableTransaction(network)
    tx.setLockTime(refundLockTime)
    val input = new TransactionInput(
      network,
      null, // parent transaction
      ScriptBuilder.createInputScript(TransactionSignature.dummy()).getProgram)
    input.setSequenceNumber(0)
    tx.addInput(input)
    tx
  }
  private val someLastOffer = ImmutableTransaction(new MutableTransaction(network))
  private val protocolConstants = ProtocolConstants()
  private val panicBlock = refundLockTime - protocolConstants.refundSafetyBlockCount
  private val peerActor = TestProbe()
  private val blockchain = TestProbe()
  private val micropaymentChannel = TestProbe()

  private def givenSuccessfulBlockchainRetrieval(): Unit = {
    peerActor.expectMsg(RetrieveBlockchainActor)
    peerActor.reply(BlockchainActorReference(blockchain.ref))
  }

  private def expectPanicNofiticationRequest(): ActorRef = {
    blockchain.expectMsg(WatchBlockchainHeight(panicBlock))
    blockchain.sender()
  }

  private def expectBroadcastReadinessRequest(block: Int): ActorRef = {
    blockchain.expectMsg(WatchBlockchainHeight(block))
    blockchain.sender()
  }

  private def givenSuccessfulBroadcast(tx: ImmutableTransaction): TransactionPublished = {
    peerActor.expectMsg(PublishTransaction(tx))
    val result = TransactionPublished(tx, tx)
    peerActor.reply(result)
    result
  }

  private def givenPanicNotification(): Unit = {
    val panicNotificationRequester = expectPanicNofiticationRequest()
    blockchain.send(panicNotificationRequester, BlockchainHeightReached(panicBlock))
  }

  private def givenLastOffer(offer: Option[ImmutableTransaction]): Unit = {
    micropaymentChannel.expectMsg(GetLastOffer)
    micropaymentChannel.reply(LastOffer(offer))
  }

  "An exchange transaction broadcast actor" should "broadcast the refund transaction if it " +
    "becomes valid" in new Fixture {
      instance ! StartBroadcastHandling(refundTx, peerActor.ref, Set(self))
      givenSuccessfulBlockchainRetrieval()
      givenPanicNotification()
      val broadcastReadyRequester = expectBroadcastReadinessRequest(refundLockTime)
      blockchain.send(broadcastReadyRequester, BlockchainHeightReached(refundLockTime))
      val result = givenSuccessfulBroadcast(refundTx)
      expectMsg(ExchangeFinished(result))
      system.stop(instance)
    }

  it should "broadcast the refund transaction if it receives a finish exchange signal" in new Fixture {
    instance ! StartBroadcastHandling(refundTx, peerActor.ref, Set(self))
    givenSuccessfulBlockchainRetrieval()
    expectPanicNofiticationRequest()
    instance ! FinishExchange
    val broadcastReadyRequester = expectBroadcastReadinessRequest(refundLockTime)
    blockchain.send(broadcastReadyRequester, BlockchainHeightReached(refundLockTime))
    val result = givenSuccessfulBroadcast(refundTx)
    expectMsg(ExchangeFinished(result))
    system.stop(instance)
  }

  it should "broadcast the last offer when the refund transaction becomes valid" in new Fixture {
    instance ! StartBroadcastHandling(refundTx, peerActor.ref, Set(self))
    instance ! SetMicropaymentActor(micropaymentChannel.ref)
    givenSuccessfulBlockchainRetrieval()
    givenPanicNotification()
    givenLastOffer(Some(someLastOffer))

    val result = givenSuccessfulBroadcast(someLastOffer)
    expectMsg(ExchangeFinished(result))
    system.stop(instance)
  }

  it should "broadcast the refund transaction if there is no last offer" in new Fixture {
    instance ! StartBroadcastHandling(refundTx, peerActor.ref, Set(self))
    instance ! SetMicropaymentActor(micropaymentChannel.ref)
    givenSuccessfulBlockchainRetrieval()
    expectPanicNofiticationRequest()
    instance ! FinishExchange
    givenLastOffer(None)
    val broadcastReadyRequester = expectBroadcastReadinessRequest(refundLockTime)
    blockchain.send(broadcastReadyRequester, BlockchainHeightReached(refundLockTime))

    val result = givenSuccessfulBroadcast(refundTx)
    expectMsg(ExchangeFinished(result))
    system.stop(instance)
  }

  trait Fixture {
    val instance: ActorRef = system.actorOf(
      Props(new ExchangeTransactionBroadcastActor(protocolConstants)))
  }
}
