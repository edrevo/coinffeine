package com.coinffeine.common.paymentprocessor

import com.coinffeine.common.currency.FiatAmount
import org.joda.time.DateTime

case class Payment(
  paymentId: String,
  senderId: String,
  receiverId: String,
  amount: FiatAmount,
  date: DateTime,
  description: String
)
