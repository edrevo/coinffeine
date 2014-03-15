package com.coinffeine.acceptance

import java.io.Closeable

import akka.actor.{ActorSystem, Props}

/** Closeable actor system used to simulate a node on the Coinffeine network.
  *
  * @constructor
  * @param supervisorProps  Properties for creating the supervisor actor
  * @param name             Actor system name
  */
class TestActorSystem(supervisorProps: Props, name: String = "default")
  extends Closeable {

  protected val system = ActorSystem(name)
  protected val supervisorRef = system.actorOf(supervisorProps, "supervisor")

  override def close(): Unit = { system.shutdown() }
}
