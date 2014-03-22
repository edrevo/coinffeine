package com.coinffeine.common.protocol.serialization

trait DefaultProtocolSerializationComponent extends ProtocolSerializationComponent {
  override lazy val protocolSerialization: ProtocolSerialization = DefaultProtocolSerialization
}
