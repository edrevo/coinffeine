package com.coinffeine.broker

import scala.math.max
import scala.concurrent.duration._

/** Tracks expiration of elements of type T over time. */
class ExpirationSchedule[T] {

  private var expirationTimes = Map[T, FiniteDuration]()

  /** Set the expiration time for an element.
    * If the element was previously tracked, reset its timeout. */
  def setExpirationFor(element: T, timeout: Duration): Unit = timeout match {
    case _: Duration.Infinite =>  // No need to keep it

    case finiteTimeout: FiniteDuration =>
      val expiration = System.currentTimeMillis().millis + finiteTimeout
      expirationTimes += element -> expiration
  }

  /** Remove expired elements.
    *
    * @return Elements that expired since the last call to expire
    */
  def removeExpired(): Set[T] = {
    val currentTime = System.currentTimeMillis().millis
    val expired = expirationTimes.collect {
      case (element, expirationTime) if expirationTime <= currentTime => element
    }
    expirationTimes --= expired
    expired.toSet
  }

  /** Time until the next tracked expiration.
    *
    * @return A finite duration or infinite if nothing is going to expire.
    */
  def timeToNextExpiration(): Duration =
    if (expirationTimes.isEmpty) Duration.Inf
    else max(0, expirationTimes.values.min.toMillis - System.currentTimeMillis).millis
}
