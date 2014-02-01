package com.bitwise.bitmarket.common.protocol

import com.bitwise.bitmarket.common.protorpc.PeerSession

trait MessageSend[T] {

  def sendAsProto(msg: T, session: PeerSession): Unit
}
