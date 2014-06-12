package com.coinffeine.gui.concurrent

/** Implicitly provide ScalaFx execution context */
object ScalaFxImplicits {
  implicit val scalaFxExecutionContext = ScalaFxExecutionContext
}
