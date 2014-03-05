package com.coinffeine.common.protocol.messages

import com.coinffeine.common.protorpc.PeerSession

trait MessageSend[T] {

  def sendAsProto(msg: T, session: PeerSession): Unit
}
