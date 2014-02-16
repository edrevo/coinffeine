package com.coinffeine.common.protocol

import com.coinffeine.common.protorpc.PeerSession

trait MessageSend[T] {

  def sendAsProto(msg: T, session: PeerSession): Unit
}
