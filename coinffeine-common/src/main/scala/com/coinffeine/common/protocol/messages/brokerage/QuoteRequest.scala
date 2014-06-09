package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.FiatCurrency
import com.coinffeine.common.protocol.messages.PublicMessage

/** Used to ask about the current quote of bitcoin traded in a given currency */
case class QuoteRequest(currency: FiatCurrency) extends PublicMessage
