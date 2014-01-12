package com.bitwise.bitmarket.common.protocol

import com.googlecode.protobuf.pro.duplex.PeerInfo

case class OrderMatch(bid: Bid, ask: Ask, counterPart: PeerInfo)
