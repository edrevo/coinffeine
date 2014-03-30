package com.coinffeine.common.protocol.serialization

import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.CoinffeineMessage
import com.coinffeine.common.protocol.messages.PublicMessage

trait ProtocolSerialization {
  def fromProtobuf(protoMessage: CoinffeineMessage): PublicMessage
  def toProtobuf(message: PublicMessage): CoinffeineMessage
}
