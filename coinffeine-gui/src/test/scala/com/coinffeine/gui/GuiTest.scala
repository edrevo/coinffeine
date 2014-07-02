package com.coinffeine.gui

import javafx.scene

import scala.concurrent.duration.FiniteDuration

import com.google.common.base.Predicate
import javafx.scene.{Node => jfxNode}
import org.loadui.testfx
import scalafx.scene.{Node, Parent}

import com.coinffeine.common.UnitTest

abstract class GuiTest[TestObject <: Parent] extends UnitTest { self =>

  var instance: TestObject = _

  trait Fixture extends testfx.GuiTest {
    override def getRootNode: scene.Parent = self.getRootNode
    setupStage()
  }

  def getRootNode: javafx.scene.Parent = {
    instance = createRootNode()
    instance
  }

  override def tags: Map[String, Set[String]] = {
    val originalTags = super.tags
    (for {
      test <- testNames
    } yield (test, originalTags.getOrElse(test, Set()) + UITestTag.name)).toMap
  }

  def createRootNode(): TestObject

  // Forwards to static GuiTest methods
  def targetWindow[T <: javafx.stage.Window](window: T) = testfx.GuiTest.targetWindow(window)
  val offset = testfx.GuiTest.offset _
  val getWindows = testfx.GuiTest.getWindows _
  val getWindowByIndex = testfx.GuiTest.getWindowByIndex _
  val findStageByTitle = testfx.GuiTest.findStageByTitle _
  def findAll(query: String) = testfx.GuiTest.findAll(query)
  def find[T <: jfxNode](selector: String, parent: Any) = testfx.GuiTest.find[T](selector, parent)
  def find[T <: jfxNode](query: String) = testfx.GuiTest.find[T](query)
  val exists = testfx.GuiTest.exists _
  val numberOf = testfx.GuiTest.numberOf _
  val captureScreenshot = testfx.GuiTest.captureScreenshot _
  def waitUntil[T <: jfxNode](value: T, condition: T => Boolean) =
    testfx.GuiTest.waitUntil[T](value, toPredicate(condition))
  def waitUntil[T <: jfxNode](value: T, condition: T => Boolean, timeout: FiniteDuration) =
    testfx.GuiTest.waitUntil[T](value, toPredicate(condition), timeout.toSeconds.toInt)
  def find[T <: jfxNode](condition: T => Boolean) =
    testfx.GuiTest.find[T](toPredicate(condition))
  def findAll[T <: jfxNode](condition: T => Boolean, parent: Node) =
    testfx.GuiTest.findAll[T](toPredicate(condition), parent)

  private def toPredicate[T](f: T => Boolean) = new Predicate[T] {
    def apply(obj: T) = f(obj)
  }
}
