package com.coinffeine.gui.control

import com.coinffeine.gui.GuiTest

import scalafx.scene.input.KeyCode

class DecimalNumberTextFieldTest extends GuiTest[DecimalNumberTextField] {
  override def createRootNode(): DecimalNumberTextField = new DecimalNumberTextField(3.141592)

  "The decimal number text field" should "start with the provided value" in new Fixture {
    instance.text.value should be ("3.141592")
  }

  it should "ignore non-numeric characters" in new Fixture {
    click(instance).`type`("abcd")
    instance.text.value should be ("3.141592")
  }

  it should "ignore characters that would make the input an invalid number" in new Fixture {
    val clickedBox = click(instance)
    clickedBox.`type`(".123")
    instance.text.value should be ("3.141592123")
    clickedBox.`type`("e-3")
    instance.text.value should be ("3.1415921233")
  }

  it should "store a zero if all characters are deleted" in new Fixture {
    click(instance).eraseCharacters(8)
    click(instance).`type`(KeyCode.ENTER)
    instance.text.value should be ("0")
  }

  it should "fail to be initialized with a negative value" in {
    an[IllegalArgumentException] should be thrownBy { new DecimalNumberTextField(-1) }
  }
}
