package com.coinffeine.acceptance

import com.google.bitcoin.params.TestNet3Params
import org.scalatest.{GivenWhenThen, fixture}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.ShouldMatchers

import com.coinffeine.acceptance.broker.TestBrokerComponent
import com.coinffeine.acceptance.peer.TestPeerComponent
import com.coinffeine.common.matchers.FutureMatchers
import com.coinffeine.common.network.NetworkComponent

/** Base trait for acceptance testing that includes a test fixture */
trait AcceptanceTest extends fixture.FeatureSpec
  with GivenWhenThen
  with Eventually
  with FutureMatchers
  with ShouldMatchers {

  class TestComponent extends TestPeerComponent with TestBrokerComponent with NetworkComponent {
    override lazy val network = TestNet3Params.get()
  }

  override type FixtureParam = TestComponent

  override def withFixture(test: OneArgTest) {
    val component = new TestComponent
    try {
      withFixture(test.toNoArgTest(component))
    } finally {
      component.broker.close()
    }
  }
}
