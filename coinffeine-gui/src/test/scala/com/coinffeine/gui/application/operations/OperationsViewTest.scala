package com.coinffeine.gui.application.operations

import javafx.scene.Node
import org.scalatest.concurrent.Eventually
import scalafx.scene.layout.Pane

import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.gui.GuiTest
import com.coinffeine.gui.application.operations.OperationsView.{OKPay, BuyOperation}

class OperationsViewTest extends GuiTest[Pane] with Eventually {
  var maybeFormData: Option[OperationsView.FormData] = None
  override def createRootNode(): Pane = {
    maybeFormData = None
    new OperationsView(d => maybeFormData = Some(d)).centerPane
  }

  "The operations view" should "not let the user submit if the bitcoin amount is zero" in new Fixture {
    doubleClick("#limit").`type`("100")
    doubleClick("#amount").`type`("0.00")
    find[Node]("#submit") should be ('disabled)
  }

  it should "not let the user submit if the bitcoin value is invalid" in new Fixture {
    doubleClick("#amount").`type`("0.000000000000001")
    find[Node]("#submit") should be ('disabled)
  }

  it should "not let the user submit if the limit value is zero" in new Fixture {
    doubleClick("#amount").`type`("0.1")
    find[Node]("#submit") should be ('disabled)
  }

  it should "not let the user submit if the limit value is invalid" in new Fixture {
    doubleClick("#amount").`type`("0.5")
    doubleClick("#limit").`type`("0.001")
    find[Node]("#submit") should be ('disabled)
  }

  it should "provide the form's data upon submission" in new Fixture {
    doubleClick("#amount").`type`("0.1")
    doubleClick("#limit").`type`("100")
    find[Node]("#submit") should not be ('disabled)
    maybeFormData should be (None)
    click("#submit")
    maybeFormData should be ('defined)
    val formData = maybeFormData.get
    formData.amount should be (0.1 BTC)
    formData.limit should be (100 EUR)
    formData.operation should be (BuyOperation)
    formData.paymentProcessor should be (OKPay)
  }
}
