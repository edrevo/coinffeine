package com.coinffeine.acceptance

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.scalatest.{GivenWhenThen, Outcome, ShouldMatchers, fixture}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Second, Seconds, Span}

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.acceptance.peer.{TestPeer, TestPeerComponent}

/** Base trait for acceptance testing that includes a test fixture */
trait AcceptanceTest extends fixture.FeatureSpec
  with GivenWhenThen
  with Eventually
  with ShouldMatchers {

  override implicit def patienceConfig = PatienceConfig(
    timeout = Span(10, Seconds),
    interval = Span(1, Second)
  )

  class IntegrationTestFixture {

    private val broker = new TestBrokerComponent().broker

    def withBroker[T](block: => T): T = try {
      Await.ready(broker.start(), Duration.Inf)
      block
    } finally {
      broker.close()
    }

    /** Loan pattern for a peer. It is guaranteed that the peers will be destroyed
      * even if the block throws exceptions.
      */
    def withPeer[T](block: TestPeer => T): T = {
      val peer = buildPeer()
      try {
        block(peer)
      } finally {
        peer.close()
      }
    }

    /** Loan pattern for a couple of peers. */
    def withPeerPair[T](block: (TestPeer, TestPeer) => T): T =
      withPeer(bob =>
        withPeer(sam =>
          block(bob, sam)
        ))

    private def buildPeer(): TestPeer =
      new TestPeerComponent().buildPeer(broker.address)
  }

  override type FixtureParam = IntegrationTestFixture

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = new IntegrationTestFixture()
    fixture.withBroker {
      withFixture(test.toNoArgTest(fixture))
    }
  }
}
