package com.coinffeine.common.protocol.serialization

import scala.util.Random

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.Currency.Bitcoin
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.exchange.{Both, Exchange}
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
    exchangeId = Exchange.Id.random(),
    amount = randomSatoshi() BTC,
    price = randomEuros() EUR,
    participants = Both(
      buyer = PeerConnection("bob", randomPort()),
      seller = PeerConnection("sam", randomPort())
    )
  )

  private def randomSatoshi() =
    Math.round(Random.nextDouble() * Bitcoin.OneBtcInSatoshi.doubleValue()) /
      Bitcoin.OneBtcInSatoshi.doubleValue()

  private def randomEuros() =
    Math.round(Random.nextDouble() * 100) / 100

  private def randomPort() = Random.nextInt(50000) + 10000
}
