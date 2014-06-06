package com.coinffeine.common.protocol.gateway

import scala.concurrent.duration.Duration

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.TestProbe
import org.scalatest.Assertions

import com.coinffeine.common.PeerConnection
import com.coinffeine.common.protocol.gateway.MessageGateway._
import com.coinffeine.common.protocol.messages.PublicMessage

/** Probe specialized on mocking a MessageGateway. */
class GatewayProbe(implicit system: ActorSystem) extends Assertions {

  /** Underlying probe used for poking actors. */
  private val probe = TestProbe()

  /** Mapping of subscriptions used to relay only what is subscribed or fail otherwise. */
  private var subscriptions: Map[ActorRef, Set[Filter]] = Map.empty

  def ref = probe.ref

  def expectSubscription(): Subscribe = {
    val subscription = try {
      probe.expectMsgClass(classOf[Subscribe])
    } catch {
      case ex: AssertionError => fail("Expected subscription failed", ex)
    }
    val currentSubscription = subscriptions.getOrElse(probe.sender, Set.empty)
    subscriptions = subscriptions.updated(probe.sender, currentSubscription + subscription.filter)
    subscription
  }

  def expectForwarding(payload: Any, dest: PeerConnection, timeout: Duration = Duration.Undefined): Unit =
    probe.expectMsgPF(timeout) {
      case message @ ForwardMessage(`payload`, `dest`) => message
    }

  def expectForwardingPF[T](dest: PeerConnection, timeout: Duration = Duration.Undefined)
                           (payloadMatcher: PartialFunction[Any, T]): T =
    probe.expectMsgPF(timeout) {
      case ForwardMessage(payload, `dest`) if payloadMatcher.isDefinedAt(payload) =>
        payloadMatcher.apply(payload)
    }

  def expectNoMsg(): Unit = probe.expectNoMsg()

  /** Relay a message to subscribed actors or make the test fail if none is subscribed. */
  def relayMessage(message: PublicMessage, origin: PeerConnection): Unit = {
    val notification = ReceiveMessage(message, origin)
    val targets = for {
      (ref, filters) <- subscriptions.toSet
      filter <- filters
      if filter(notification)
    } yield ref
    assert(!targets.isEmpty, s"No one is expecting $notification, check subscription filters")
    targets.foreach { target =>
      probe.send(target, notification)
    }
  }
}
