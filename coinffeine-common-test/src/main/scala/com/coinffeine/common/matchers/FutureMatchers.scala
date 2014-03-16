package com.coinffeine.common.matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import org.scalatest.matchers.{MatchResult, Matcher}
import java.util.concurrent.TimeoutException

/** Scalatest matchers for futures */
trait FutureMatchers {

  /** Maximum time to wait for a future when matching for it. */
  case class MatchTimeout(timeout: FiniteDuration)

  /** Default matching timeout implicitly used. You can override it by defining an implicit
    * MatchTimeout in any scope closer to the matches.
    *
    * For instance:
    *
    * {{{
    * class FooTest extends FlatSpec with ShouldMatchers with FutureMatchers {
    *   // Override for the whole test suite
    *   implicit val timeout = MatchTimeout(10.seconds)
    *   ...
    *
    *   it should "foo" in {
    *     // Only for this test
    *     implicit val timeout = MatchTimeout(1.second)
    *     ...
    *   }
    * }
    * }}}
    */
  implicit val defaultTimeout = MatchTimeout(3.seconds)

  /** Matches with futures that succeed with a value satisfying a matcher. */
  def eventually[T](matcher: Matcher[T])(implicit t: MatchTimeout): Matcher[Future[T]] =
    new Matcher[Future[T]] {
      override def apply(future: Future[T]) = {
        try {
          Await.ready(future, t.timeout).value.get match {
            case Failure(problem) => new MatchResult(
              matches = false,
              failureMessage = s"future failed with $problem",
              negatedFailureMessage = s"future did not failed with $problem"
            )
            case Success(result) => matcher(result)
          }
        } catch {
          case _: TimeoutException => new MatchResult(
            matches = false,
            failureMessage =  s"future timed out after ${t.timeout}",
            negatedFailureMessage = s"future didn't timed out after ${t.timeout}"
          )
          case _: InterruptedException => new MatchResult(
            matches = false,
            failureMessage =  s"future was interrupted",
            negatedFailureMessage = s"future was not interrupted"
          )
        }
      }
    }
}
