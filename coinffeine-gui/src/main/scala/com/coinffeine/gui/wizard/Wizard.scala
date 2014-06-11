package com.coinffeine.gui.wizard

import scalafx.Includes._
import scalafx.beans.property.{IntegerProperty, ObjectProperty}
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{AnchorPane, BorderPane, HBox}
import scalafx.scene.text.Font
import scalafx.stage.Stage

/** Step-by-step wizard that accumulates information of type Data.
  *
  * @param steps        Sequence of wizard steps
  * @param initialData  Initial value for the wizard
  * @param title        Wizard title
  * @param width        Window width
  * @param height       Window height
  * @tparam Data        Type of the wizard result
  */
class Wizard[Data](steps: Seq[Step[Data]], initialData: Data, title: String,
                   width: Double = 540, height: Double = 320) {

  private val data = new ObjectProperty[Data](this, "wizardData", initialData)
  private val stepNumber = steps.size
  private val currentStep = new IntegerProperty(this, "currentStep", 1)

  steps.foreach(page => page.bindTo(data))

  def show(): Data = {
    stage.showAndWait()
    data.value
  }

  private val wizardHeader = {
    val progress = new ProgressIndicator(stepNumber, currentStep).pane
    val title = new Label("Initial setup") { font = Font(18) }
    new AnchorPane {
      prefHeight = 50
      prefWidth = Wizard.this.width
      content = Seq(title, progress)
      AnchorPane.setTopAnchor(title, 15)
      AnchorPane.setLeftAnchor(title, 22)
      AnchorPane.setTopAnchor(progress, 20)
      AnchorPane.setRightAnchor(progress, 15)
    }
  }

  private val wizardFooter = {
    val backButton = new Button("< Back") {
      visible <== currentStep =!= 1
      handleEvent(ActionEvent.ACTION) { () => changeToStep(currentStep.value - 1) }
    }

    val nextButton = new Button {
      text <== when(currentStep === stepNumber) choose "Finish" otherwise "Next >"
      handleEvent(ActionEvent.ACTION) { () =>
        if (currentStep.value < stepNumber) changeToStep(currentStep.value + 1) else stage.close()
      }
    }

    val buttonBox = new HBox(spacing = 5) {
      content = Seq(backButton, nextButton)
    }

    new AnchorPane {
      prefHeight = 44
      prefWidth = Wizard.this.width
      content = buttonBox
      AnchorPane.setTopAnchor(buttonBox, 5)
      AnchorPane.setRightAnchor(buttonBox, 15)
    }
  }

  private val rootWizardPane: BorderPane = new BorderPane {
    top = wizardHeader
    center = steps.head.pane
    bottom = wizardFooter
  }

  private val stage = new Stage {
    title = Wizard.this.title
    scene = new Scene(Wizard.this.width, Wizard.this.height) {
      root = rootWizardPane
    }
  }

  private def changeToStep(index: Int): Unit = {
    currentStep.value = index
    rootWizardPane.center = steps(index - 1).pane
  }
}

