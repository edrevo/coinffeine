package com.coinffeine.common.bitcoin

import com.coinffeine.common.{BitcoinjTest, UnitTest}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.Implicits._

class PimpMyWalletTest extends UnitTest with BitcoinjTest {

  "Pimp my wallet" must "block funds on create send" in new Fixture {
    val tx = instance.blockFunds(someoneElse, 1.BTC)
    instance.balance() should be (initialFunds - instance.valueSentFromMe(tx))
  }

  it must "release funds previously blocked on create send" in new Fixture {
    val tx = instance.blockFunds(someoneElse, 1.BTC)
    instance.releaseFunds(tx)
    instance.balance() should be (initialFunds)
  }

  it must "block funds after release" in new Fixture {
    val tx1 = instance.blockFunds(someoneElse, 1.BTC)
    instance.releaseFunds(tx1)
    val tx2 = instance.blockFunds(someoneElse, initialFunds)
    instance.balance() should be (0.BTC)
  }

  trait Fixture {
    val keyPair = new KeyPair
    val someoneElse = new KeyPair().toAddress(network)
    val instance = createWallet(keyPair, 10.BTC)
    val initialFunds = instance.balance()
  }
}
