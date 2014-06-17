package com.coinffeine.gui.application

import scalafx.scene.layout.Pane

/** Represents a view on the application */
trait ApplicationView {

  /** Name of the view */
  def name: String

  /** Pane to be displayed at the center area of the application when this view is active */
  def centerPane: Pane
}
