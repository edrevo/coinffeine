package com.coinffeine.gui

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

import scalafx.application.Platform

package object concurrent {

  /** Use this context to update ScalaFX nodes safely */
  val ScalaFxExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new Executor {
    override def execute(command: Runnable): Unit = {
      Platform.runLater(command)
    }
  })
}
