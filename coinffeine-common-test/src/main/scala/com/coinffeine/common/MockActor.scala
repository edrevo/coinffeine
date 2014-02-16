package com.coinffeine.common

import akka.actor._
import akka.testkit.TestProbe

class MockActor(master: ActorRef) extends Actor with ActorLogging {

  import MockActor._

  override def preStart() { master ! MockStarted(self) }
  override def postStop() { master ! MockStopped(self) }
  override def postRestart(reason: Throwable) {
    master ! MockRestarted(self, reason)
  }

  def receive: Actor.Receive = {
    case MockSend(target, message) => target ! message
    case MockThrow(ex) => throw ex
    case message if sender != master =>
      master ! MockReceived(self, sender, message)
    case unexpectedMessage =>
      log.warning("Unexpected message {} received by a mock actor", unexpectedMessage)
  }
}

object MockActor {
  def props(master: ActorRef): Props = Props(new MockActor(master))
  def props(probe: TestProbe): Props = props(probe.ref)

  case class MockStarted(ref: ActorRef)
  case class MockStopped(ref: ActorRef)
  case class MockRestarted(ref: ActorRef, reason: Throwable)
  case class MockReceived(ref: ActorRef, sender: ActorRef, message: Any)
  case class MockSend(target: ActorRef, message: Any)
  case class MockThrow(ex: Throwable)
}
