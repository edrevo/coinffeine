package com.coinffeine.gui.util

import javafx.concurrent.Task
import org.controlsfx.dialog.Dialogs

class ProgressDialog(title: String, message: String, actions: (() => Unit)*) {

  private object task extends Task[Unit] {
    override def call(): Unit = {
      val numberOfActions = actions.size
      if (numberOfActions > 1) { updateProgress(0, numberOfActions) }
      else { updateProgress(-1, -1) }
      actions.zipWithIndex.foreach {
        case (action, index) =>
          action()
          updateProgress(index + 1, numberOfActions)
      }

      // sleep a second to given time to the user to see the task completion
      Thread.sleep(1000)
    }
  }

  def show(): Unit = {
    val thread = new Thread(task)
    thread.setDaemon(true)
    thread.start()
    Dialogs.create()
      .title(title)
      .message(message)
      .showWorkerProgress(task)
  }
}

object ProgressDialog {

  def apply(title: String, message: String)(action: => Unit): ProgressDialog =
    new ProgressDialog(title, message, () => action)
}
