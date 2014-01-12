package com.bitwise.bitmarket

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.matchers.MustMatchers

/** FlatSpec configured to test Akka actors. */
abstract class AkkaSpec(actorSystem: ActorSystem = ActorSystem("TestSystem"))
  extends TestKit(actorSystem) with FlatSpec with BeforeAndAfterAll with MustMatchers
  with ImplicitSender {

  def this(systemName: String) = this(ActorSystem(systemName))

  override def afterAll() {
    system.shutdown()
  }
}

object AkkaSpec {

  /** Create an actor system with logging interception enabled.
    *
    * @param name  Name of the actor system
    *
    * See [[akka.testkit.EventFilter]] for more details.
    */
  def systemWithLoggingInterception(name: String): ActorSystem =
    ActorSystem(name, ConfigFactory.parseString(
      """
        |akka {
        |   loggers = ["akka.testkit.TestEventListener"]
        |   mode = "test"
        |}
      """.stripMargin
    ))
}
