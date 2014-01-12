package com.bitwise.bitmarket.common.protocol

case class OfferId(bytes: Int) {
  override def toString: String = bytes.toHexString
}
