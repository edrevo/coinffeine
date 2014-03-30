package com.coinffeine.common.protocol.serialization

import com.google.protobuf.Message

private[serialization] object ProtoMapping {
  def fromProtobuf[T, M <: Message](message: M)(implicit mapping: ProtoMapping[T, M]): T =
    mapping.fromProtobuf(message)

  def toProtobuf[T, M <: Message](obj: T)(implicit mapping: ProtoMapping[T, M]): M =
    mapping.toProtobuf(obj)
}

/** Represents how to map between a domain logic case class and a protobuf message */
private[serialization] trait ProtoMapping[T, M <: Message] {
  def toProtobuf(obj: T): M
  def fromProtobuf(message: M): T
}
