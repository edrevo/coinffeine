package com.coinffeine.common.network

import com.coinffeine.common.bitcoin.Network

trait NetworkComponent {
  def network: Network
}
