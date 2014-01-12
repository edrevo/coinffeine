package com.bitwise.bitmarket.common.protocol

case class PeerId(address: String) {
  override def toString = address
}
