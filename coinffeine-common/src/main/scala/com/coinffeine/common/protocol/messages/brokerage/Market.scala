package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.FiatCurrency

/** Identifies a given market. */
case class Market[+C <: FiatCurrency](currency: C)
