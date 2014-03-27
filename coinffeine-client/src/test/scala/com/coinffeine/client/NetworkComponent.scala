package com.coinffeine.client

import com.google.bitcoin.core.NetworkParameters

trait NetworkComponent {
  val network: NetworkParameters
}
