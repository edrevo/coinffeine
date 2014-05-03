package com.coinffeine.common.protocol.serialization

import java.util.Currency
import scala.util.Random

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.currency.{BtcAmount, FiatAmount}
import com.coinffeine.common.protocol.ProtocolConstants
import com.coinffeine.common.protocol.messages.PublicMessage
import com.coinffeine.common.protocol.messages.brokerage.OrderMatch
import com.coinffeine.common.protocol.protobuf.CoinffeineProtobuf.CoinffeineMessage

/** Provides a serialization that behaves like the default one but allowing injection of
  * serialization errors and other testing goodies.
  */
trait TestProtocolSerializationComponent extends ProtocolSerializationComponent {
  this: ProtocolConstants.Component =>

  override lazy val protocolSerialization = new TestProtocolSerialization(protocolConstants.version)

  def randomMessageAndSerialization(): (PublicMessage, CoinffeineMessage) = {
    val message = randomMessage()
    (message, protocolSerialization.toProtobuf(message))
  }

  def randomMessage(): OrderMatch = OrderMatch(
    exchangeId = s"exchange-${Random.nextLong().toHexString}",
    amount = new BtcAmount(BigDecimal(randomSatoshi())),
    price = new FiatAmount(BigDecimal(Random.nextDouble()), Currency.getInstance("EUR")),
    buyer = PeerConnection("bob", randomPort()),
    seller = PeerConnection("sam", randomPort())
  )

  private def randomSatoshi() =
    Math.round(Random.nextDouble() * BtcAmount.OneBtcInSatoshi.doubleValue()) /
      BtcAmount.OneBtcInSatoshi.doubleValue()

  private def randomPort() = Random.nextInt(50000) + 10000
}
