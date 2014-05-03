package com.coinffeine.common.protocol

case class Version(major: Int, minor: Int) {
  override def toString = s"$major.$minor"
}
