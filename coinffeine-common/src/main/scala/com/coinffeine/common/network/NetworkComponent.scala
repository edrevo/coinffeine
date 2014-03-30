package com.coinffeine.common.network

import com.google.bitcoin.core.NetworkParameters

trait NetworkComponent {
  def network: NetworkParameters
}
