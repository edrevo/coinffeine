package com.coinffeine.gui.wizard

import scalafx.beans.property.ObjectProperty
import scalafx.scene.layout.Pane

/** An step of a wizard */
trait Step[Data] {
  def pane: Pane
  def bindTo(data: ObjectProperty[Data]): Unit
}
