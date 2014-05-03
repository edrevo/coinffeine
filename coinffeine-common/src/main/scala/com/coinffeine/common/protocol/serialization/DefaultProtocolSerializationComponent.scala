package com.coinffeine.common.protocol.serialization

import com.coinffeine.common.network.NetworkComponent
import com.coinffeine.common.protocol.ProtocolConstants

trait DefaultProtocolSerializationComponent extends ProtocolSerializationComponent {
  this: NetworkComponent with ProtocolConstants.Component =>

  override def protocolSerialization: ProtocolSerialization =
    new DefaultProtocolSerialization(protocolConstants.version, new TransactionSerialization(network))
}
