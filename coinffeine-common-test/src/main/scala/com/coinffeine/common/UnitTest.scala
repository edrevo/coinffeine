package com.coinffeine.common

import org.scalatest.{BeforeAndAfter, FlatSpec, ShouldMatchers}

/** Default base class for unit tests that mixes the most typical testing traits. */
abstract class UnitTest extends FlatSpec with ShouldMatchers with BeforeAndAfter
