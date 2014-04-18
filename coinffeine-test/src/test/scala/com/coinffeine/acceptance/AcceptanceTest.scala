package com.coinffeine.acceptance

import com.google.bitcoin.params.TestNet3Params
import org.scalatest.{GivenWhenThen, Outcome, ShouldMatchers, fixture}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Second, Seconds, Span}

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.acceptance.peer.TestPeerComponent
import com.coinffeine.common.network.NetworkComponent
import com.coinffeine.common.protocol.ProtocolConstants

/** Base trait for acceptance testing that includes a test fixture */
trait AcceptanceTest extends fixture.FeatureSpec
  with GivenWhenThen
  with Eventually
  with ShouldMatchers {

  override implicit def patienceConfig = PatienceConfig(
    timeout = Span(10, Seconds),
    interval = Span(1, Second)
  )

  class TestComponent extends TestPeerComponent
    with TestBrokerComponent with NetworkComponent with ProtocolConstants.Component {

    override lazy val network = TestNet3Params.get()
    override val protocolConstants = ProtocolConstants.DefaultConstants
  }

  override type FixtureParam = TestComponent

  override def withFixture(test: OneArgTest): Outcome = {
    val component = new TestComponent
    try {
      withFixture(test.toNoArgTest(component))
    } finally {
      component.broker.close()
    }
  }
}
