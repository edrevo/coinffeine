package com.coinffeine.common.protocol.serialization

import com.coinffeine.common.network.NetworkComponent

trait DefaultProtocolSerializationComponent extends ProtocolSerializationComponent {
  this: NetworkComponent =>

  override def protocolSerialization: ProtocolSerialization =
    new DefaultProtocolSerialization(new TransactionSerialization(network))
}
