package com.coinffeine.common.protorpc

import com.googlecode.protobuf.pro.duplex.{ClientRpcController, RpcClientChannel}

trait PeerSession extends AutoCloseable {
  def channel: RpcClientChannel
  def controller: ClientRpcController
}
