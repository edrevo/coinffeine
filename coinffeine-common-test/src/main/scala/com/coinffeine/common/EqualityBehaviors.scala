package com.coinffeine.common

trait EqualityBehaviors { this: UnitTest =>

  def respectingEqualityProperties[T](equivalenceClasses: Seq[Seq[T]]): Unit = {
    it should "be equal with the ones in the same equivalence class" in {
      for (equivalenceClass <- equivalenceClasses;
           Seq(a, b) <- equivalenceClass.sliding(2)) {
        a should equal (b)
      }
    }

    it should "be different to the ones in other equivalence classes" in {
      for (Seq(classA, classB) <- equivalenceClasses.sliding(2)) {
        classA.head should not equal classB.head
      }
    }

    it should "have the same hash when being in the same partition" in {
      for (equivalenceClass <- equivalenceClasses) {
        equivalenceClass.map(_.hashCode()).toSet should have size 1
      }
    }
  }
}
