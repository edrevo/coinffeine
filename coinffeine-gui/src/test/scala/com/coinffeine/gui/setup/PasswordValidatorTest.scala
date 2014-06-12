package com.coinffeine.gui.setup

import com.coinffeine.common.UnitTest

class PasswordValidatorTest extends UnitTest {

  val validator = new PasswordValidator

  "A password validator" should "consider weak small length passwords" in {
    validator.isWeak("short") should be (true)
  }

  it should "consider strong long passwords" in {
    validator.isWeak("0fQ:7@8G2(T8'G20u+We") should be (false)
  }
}
