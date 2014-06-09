package com.coinffeine.common.protocol.messages.brokerage

import com.coinffeine.common.Currency.Implicits._

import com.coinffeine.common.UnitTest

class VolumeByPriceTest extends UnitTest {

  "Volume by price" should "contain to a non-negative amounts" in {
    val ex = the [IllegalArgumentException] thrownBy {
      VolumeByPrice(470.EUR -> -1.BTC)
    }
    ex.getMessage should include ("Amount ordered must be strictly positive")
  }

  it should "have non-negative price" in {
    val ex = the [IllegalArgumentException] thrownBy {
      VolumeByPrice(-470.EUR -> 1.BTC)
    }
    ex.getMessage should include ("Price must be strictly positive")
  }

  it should "be empty when having no volume at all" in {
    VolumeByPrice.empty should be ('empty)
    VolumeByPrice(100.EUR -> 0.1.BTC) should not be 'empty
  }

  it should "have highest and lowest prices when non empty" in {
    val nonEmpty = VolumeByPrice(100.EUR -> 0.1.BTC, 150.EUR -> 0.3.BTC, 200.EUR -> 4.BTC)
    nonEmpty.lowestPrice should be (Some(100.EUR))
    nonEmpty.highestPrice should be (Some(200.EUR))
  }

  it should "have no highest and lowest prices when empty" in {
    val empty = VolumeByPrice.empty
    empty.lowestPrice should be ('empty)
    empty.highestPrice should be ('empty)
  }

  it should "return the volume at any price" in {
    val volume = VolumeByPrice(100.EUR -> 0.1.BTC)
    volume.volumeAt(100.EUR) should be (0.1.BTC)
    volume.volumeAt(101.EUR) should be (0.BTC)
  }

  it should "be increased by some amount at a given price" in {
    val volume = VolumeByPrice(100.EUR -> 0.1.BTC)
      .increase(100.EUR, 0.9.BTC)
      .increase(200.EUR, 0.5.BTC)
    volume.volumeAt(100.EUR) should be (1.BTC)
    volume.volumeAt(200.EUR) should be (0.5.BTC)
  }

  it should "be decreased by some amount at a given price" in {
    val volume = VolumeByPrice(100.EUR -> 2.BTC, 200.EUR -> 1.BTC)
      .decrease(100.EUR, 1.BTC)
      .decrease(200.EUR, 1.BTC)
      .decrease(300.EUR, 1.BTC)
    volume.volumeAt(100.EUR) should be (1.BTC)
    volume.volumeAt(200.EUR) should be (0.BTC)
    volume.volumeAt(300.EUR) should be (0.BTC)
  }
}
