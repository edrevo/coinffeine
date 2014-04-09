package com.coinffeine.common.protocol.messages.brokerage

import java.util.Currency

import com.coinffeine.common.protocol.messages.PublicMessage

case class CancelOrder(currency: Currency) extends PublicMessage
