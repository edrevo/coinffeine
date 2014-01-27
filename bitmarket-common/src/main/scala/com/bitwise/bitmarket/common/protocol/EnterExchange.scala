package com.bitwise.bitmarket.common.protocol

import com.google.bitcoin.core.Transaction

case class EnterExchange(
  exchangeId: String,
  commitmentTransaction: Transaction
)
