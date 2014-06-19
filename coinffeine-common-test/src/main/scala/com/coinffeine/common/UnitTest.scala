package com.coinffeine.common

import akka.actor.ActorSystem
import akka.testkit.TestKitExtension
import org.scalatest.{BeforeAndAfter, FlatSpec, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures

/** Default base class for unit tests that mixes the most typical testing traits. */
abstract class UnitTest extends FlatSpec with ShouldMatchers with BeforeAndAfter with ScalaFutures {
  override def spanScaleFactor: Double =
    TestKitExtension.get(ActorSystem()).TestTimeFactor
}
