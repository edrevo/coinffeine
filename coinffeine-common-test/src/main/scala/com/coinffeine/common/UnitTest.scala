package com.coinffeine.common

import akka.actor.ActorSystem
import akka.testkit.TestKitExtension
import org.scalatest.{BeforeAndAfter, FlatSpecLike, ShouldMatchers}
import org.scalatest.concurrent.ScalaFutures

/** Default trait for unit tests that mixes the most typical testing traits. */
trait UnitTest extends FlatSpecLike with ShouldMatchers with BeforeAndAfter with ScalaFutures {
  override def spanScaleFactor = TestKitExtension.get(ActorSystem()).TestTimeFactor
}
