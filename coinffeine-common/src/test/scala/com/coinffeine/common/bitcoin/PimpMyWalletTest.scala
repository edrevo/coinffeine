package com.coinffeine.common.bitcoin

import scala.util.Random

import com.coinffeine.common.{BitcoinjTest, UnitTest}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.Implicits._

class PimpMyWalletTest extends UnitTest with BitcoinjTest {

  "Pimp my wallet" must "block funds on create send" in new Fixture {
    val tx = instance.blockFunds(someoneElseAddress, 1.BTC)
    instance.value(tx) should be (-1.BTC)
    instance.balance() should be (initialFunds - instance.valueSentFromMe(tx))
  }

  it must "block funds in create multisign transaction" in new Fixture {
    val tx = instance.blockMultisignFunds(Seq(keyPair, someoneElsekeyPair), 1.BTC)
    instance.value(tx) should be (-1.BTC)
    instance.balance() should be (initialFunds - instance.valueSentFromMe(tx))
  }

  it must "release funds previously blocked on create send" in new Fixture {
    val tx = instance.blockFunds(someoneElseAddress, 1.BTC)
    instance.releaseFunds(tx)
    instance.balance() should be (initialFunds)
  }

  it must "block funds after release" in new Fixture {
    val tx1 = instance.blockFunds(someoneElseAddress, 1.BTC)
    instance.releaseFunds(tx1)
    val tx2 = instance.blockFunds(someoneElseAddress, initialFunds)
    instance.balance() should be (0.BTC)
  }

  it must "consider change funds after tx is broadcast" in new Fixture {
    val tx = instance.blockFunds(someoneElseAddress, 1.BTC)
    sendToBlockChain(tx)
    instance.balance() should be (initialFunds - 1.BTC)
  }

  it must "support a period of arbitrary spent or release operations" in new Fixture {
    var expectedBalance = initialFunds
    for (_ <- 1 to 30) {
      val tx = instance.blockFunds(someoneElseAddress, 0.1.BTC)
      if (Random.nextBoolean()) {
        sendToBlockChain(tx)
        expectedBalance += instance.value(tx)
      } else {
        instance.releaseFunds(tx)
      }
    }
    instance.balance() should be (expectedBalance)
  }

  trait Fixture {
    val keyPair = new KeyPair
    val someoneElsekeyPair = new KeyPair
    val someoneElseAddress = someoneElsekeyPair.toAddress(network)
    val instance = createWallet(keyPair, 10.BTC)
    val initialFunds = instance.balance()
  }
}
