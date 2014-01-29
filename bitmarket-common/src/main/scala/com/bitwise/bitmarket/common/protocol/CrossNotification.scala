package com.bitwise.bitmarket.common.protocol

/**
 * Represents the ask/bid cross information from two peers.
 *
 * @param exchangeId ID of Exchange. Is used in all messages on this exchange.
 * @param cross OrderMatch object which contains all information about this ask/bid cross.
 */
case class CrossNotification (
  exchangeId: String,
  cross: OrderMatch
)
