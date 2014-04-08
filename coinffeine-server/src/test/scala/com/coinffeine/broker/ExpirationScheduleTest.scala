package com.coinffeine.broker

import scala.concurrent.duration._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ExpirationScheduleTest extends FlatSpec with ShouldMatchers {

  private class TestSchedule extends ExpirationSchedule[String]

  "An expiration schedule" should "expire no elements and have infinite timeout when empty" in {
    val emptySchedule = new TestSchedule()
    emptySchedule.removeExpired() should be ('empty)
    emptySchedule.timeToNextExpiration() should not(be('finite))
  }

  it should "expire no elements when adding elements of infinite expiration" in {
    val schedule = new TestSchedule()
    schedule.setExpirationFor("infinite", Duration.Inf)
    schedule.removeExpired() should be ('empty)
    schedule.timeToNextExpiration() should not(be('finite))
  }

  it should "expire elements" in {
    val schedule = new TestSchedule()
    schedule.setExpirationFor("1 second", 1.second)
    schedule.setExpirationFor("1 minute", 1.minute)
    Thread.sleep(2.seconds.toMillis)
    schedule.removeExpired() should be (Set("1 second"))
  }

  it should "compute the next expiration time" in {
    val schedule = new TestSchedule()
    schedule.setExpirationFor("1 day", 1.day)
    schedule.setExpirationFor("2 days", 2.days)
    schedule.setExpirationFor("10 days", 10.days)
    schedule.timeToNextExpiration().toHours should (be(24) or be(23))
  }
}
