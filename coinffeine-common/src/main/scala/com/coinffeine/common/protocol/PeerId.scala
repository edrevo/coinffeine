package com.coinffeine.common.protocol

case class PeerId(address: String) {
  override def toString = address
}
