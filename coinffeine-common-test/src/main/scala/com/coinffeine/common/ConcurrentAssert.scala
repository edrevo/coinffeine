package com.coinffeine.common

import java.lang.Throwable
import scala.util.control.Breaks._

object ConcurrentAssert {

  val defaultTimeout: Int = 1000
  val defaultRetryWait: Int = 100

  /**
   * Repeat an assertion until it doesn't fail or a timeout is reached.
   *
   * @param assertion        Assertion to check
   * @param timeoutInMillis  Timeout
   * @param retryWaitMillis  Time to wait between attempts
   */
  def assertEventually(assertion: Runnable, timeoutInMillis: Int, retryWaitMillis: Int) {
    val timeLimit: Long = System.currentTimeMillis + timeoutInMillis
    var lastException: Throwable = null
    do {
      try {
        assertion.run
      }
      catch {
        case e: RuntimeException => {
          lastException = e
        }
        case e: AssertionError => {
          lastException = e
        }
      }
      if (System.currentTimeMillis >= timeLimit) {
        break
      }
      try {
        Thread.sleep(retryWaitMillis)
      }
      catch {
        case e: InterruptedException => {
          throw new RuntimeException(e)
        }
      }
    } while (System.currentTimeMillis < timeLimit)
    throw new RuntimeException(lastException)
  }

  /**
   * Repeat an assertion until it doesn't fail or a timeout is reached. Default values for
   * the timeout and retry wait are used.
   *
   * @param assertion  Assertion to check
   * @see #assertEventually(Runnable, int, int)
   */
  def assertEventually(assertion: Runnable) {
    assertEventually(assertion, defaultTimeout, defaultRetryWait)
  }
}
