package com.coinffeine.common.network

import com.google.bitcoin.params.MainNetParams

trait MainNetComponent extends NetworkComponent {
  override lazy val network: MainNetParams = MainNetParams.get
}
