package com.coinffeine.common.paymentprocessor

import org.joda.time.DateTime

import com.coinffeine.common.{CurrencyAmount, FiatCurrency}

case class Payment[C <: FiatCurrency](
  id: String,
  senderId: String,
  receiverId: String,
  amount: CurrencyAmount[C],
  date: DateTime,
  description: String
)
