package com.bitwise.bitmarket.common.protorpc

import com.google.protobuf.RpcCallback

object Callbacks {

  def noop[T]: RpcCallback[T] = new RpcCallback[T] { def run(parameter: T) {} }

  /** This convenience function is needed to invoke noop() from Java code. */
  @Deprecated
  def noop[T](clazz: Class[T]): RpcCallback[T] = new RpcCallback[T] { def run(parameter: T) {} }
}
