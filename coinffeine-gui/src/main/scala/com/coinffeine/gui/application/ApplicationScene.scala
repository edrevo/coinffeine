package com.coinffeine.gui.application

import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane

import com.coinffeine.gui.application.ApplicationScene._

/** Main scene of the application
  *
  * @param views  Available application views. The first one is visible at application start.
  */
class ApplicationScene(views: Seq[ApplicationView])
  extends Scene(width = DefaultWidth, height = DefaultHeight) {
  root = new BorderPane {
    center = views.head.centerPane
  }
}

object ApplicationScene {
  val DefaultWidth = 600
  val DefaultHeight = 400
}
