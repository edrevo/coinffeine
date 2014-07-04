package com.coinffeine.gui.setup

import javafx.scene.control.TextField

import com.coinffeine.gui.GuiTest

class TopUpStepPaneTest extends GuiTest[TopUpStepPane] {

  val address = "0xDEADBEEF"
  override def createRootNode(): TopUpStepPane = new TopUpStepPane(address)

  "The top up step pane" should "show the provided address email or password" in new Fixture {
    val addressField = find[TextField]("#address")
    addressField should not be ('editable)
    addressField.getText should be (address)
  }
}
