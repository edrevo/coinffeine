package com.coinffeine.common.protocol.serialization

import com.coinffeine.common.network.CoinffeineUnitTestNetwork
import com.coinffeine.common.protocol.Version
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.CoinffeineMessage

class TestProtocolSerialization(version: Version) extends ProtocolSerialization {
  private val underlying = new DefaultProtocolSerialization(
    version, new TransactionSerialization(CoinffeineUnitTestNetwork))
  var unserializableMessages = Set.empty[PublicMessage]
  var undeserializableMessages = Set.empty[CoinffeineMessage]

  override def toProtobuf(message: PublicMessage) =
    if (unserializableMessages.contains(message))
      throw new IllegalArgumentException("Cannot serialize")
    else underlying.toProtobuf(message)

  override def fromProtobuf(protoMessage: CoinffeineMessage) =
    if (undeserializableMessages.contains(protoMessage))
      throw new IllegalArgumentException("Cannot deserialize")
    else underlying.fromProtobuf(protoMessage)

  def wontSerialize(message: PublicMessage): Unit = unserializableMessages += message

  def wontDeserialize(protoMessage: CoinffeineMessage): Unit =
    undeserializableMessages += protoMessage
}
