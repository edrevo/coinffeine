package com.coinffeine.acceptance

import com.coinffeine.client.api.CoinffeineNetwork.{Connected, Disconnected}

/** Test the network connection lifecycle. */
class ConnectionTest extends AcceptanceTest {

  feature("A peer can manage its connection with the network") {

    scenario("A disconnected peer can connect to the broker") { f =>
      f.withPeer { peer =>
        Given("a peer is disconnected")
        peer.network.status should be (Disconnected)

        When("trying to connect")
        val attempt = peer.network.connect()

        Then("it gets connected to the broker")
        attempt.futureValue should be (Connected)
      }
    }
  }
}
