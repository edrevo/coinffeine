package com.coinffeine.acceptance

import com.google.bitcoin.params.TestNet3Params

import com.coinffeine.common.network.NetworkComponent

trait IntegrationTestNetworkComponent extends NetworkComponent {
  override def network = TestNet3Params.get()
}
