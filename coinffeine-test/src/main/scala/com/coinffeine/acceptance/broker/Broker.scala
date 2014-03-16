package com.coinffeine.acceptance.broker

import java.io.Closeable

import com.coinffeine.common.PeerConnection

trait Broker extends Closeable {
  def address: PeerConnection
}
