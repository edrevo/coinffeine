package com.coinffeine.acceptance

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.scalatest.{GivenWhenThen, Outcome, ShouldMatchers, fixture}
import org.scalatest.concurrent.{ScalaFutures, Eventually}
import org.scalatest.time.{Second, Seconds, Span}

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.client.api.CoinffeineApp
import com.coinffeine.common.protocol.ProtocolConstants

/** Base trait for acceptance testing that includes a test fixture */
trait AcceptanceTest extends fixture.FeatureSpec
  with GivenWhenThen
  with Eventually
  with ShouldMatchers
  with ScalaFutures {

  /** Easy to override protocol constants used during the acceptance test */
  def protocolConstants: ProtocolConstants = ProtocolConstants.DefaultConstants

  override implicit def patienceConfig = PatienceConfig(
    timeout = Span(10, Seconds),
    interval = Span(1, Second)
  )

  class IntegrationTestFixture {

    private val broker = new TestBrokerComponent(protocolConstants).broker
    Await.ready(broker.start(), Duration.Inf)

    /** Loan pattern for a peer. It is guaranteed that the peers will be destroyed
      * even if the block throws exceptions.
      */
    def withPeer[T](block: CoinffeineApp => T): T = {
      val peer = buildPeer()
      try {
        block(peer)
      } finally {
        peer.close()
      }
    }

    /** Loan pattern for a couple of peers. */
    def withPeerPair[T](block: (CoinffeineApp, CoinffeineApp) => T): T =
      withPeer(bob =>
        withPeer(sam =>
          block(bob, sam)
        ))

    private[AcceptanceTest] def close(): Unit = {
      broker.close()
    }

    private def buildPeer() = new TestCoinffeineApp(broker.address, protocolConstants).app
  }

  override type FixtureParam = IntegrationTestFixture

  override def withFixture(test: OneArgTest): Outcome = {
    val fixture = new IntegrationTestFixture()
    try {
      withFixture(test.toNoArgTest(fixture))
    } finally {
      fixture.close()
    }
  }
}
