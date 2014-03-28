package com.coinffeine.common.network

trait UnitTestNetworkComponent extends NetworkComponent {
  override lazy val network  = CoinffeineUnitTestParams
}
