package com.coinffeine.gui.setup

/** Criterion for detecting weak passwords.
  *
  * Right now it just checks password length. Can be extended to check for character classes and
  * dictionary words.
  */
class PasswordValidator {
  def isWeak(password: String): Boolean = password.size < 8
}
