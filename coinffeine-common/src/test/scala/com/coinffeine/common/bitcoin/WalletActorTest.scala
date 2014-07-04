package com.coinffeine.common.bitcoin

import akka.actor.Props
import org.scalatest.concurrent.Eventually

import com.coinffeine.common.{system, AkkaSpec, BitcoinjTest}
import com.coinffeine.common.Currency.Implicits._
import com.coinffeine.common.bitcoin.Implicits._

class WalletActorTest extends AkkaSpec("WalletActorTest") with BitcoinjTest with Eventually {

  "The wallet actor" must "block funds in a multisign transaction" in new Fixture {
    val request = WalletActor.BlockFundsInMultisign(Seq(keyPair, otherKeyPair), 1.BTC)
    instance ! request
    expectMsgPF() {
      case WalletActor.FundsBlocked(`request`, tx) => wallet.value(tx) should be (-1.BTC)
    }
  }

  it must "fail to block funds when there is no enough amount" in new Fixture {
    val request = WalletActor.BlockFundsInMultisign(Seq(keyPair, otherKeyPair), 10000.BTC)
    instance ! request
    expectMsgPF() {
      case WalletActor.FundsBlockingError(`request`, _: IllegalArgumentException) =>
    }
  }

  it must "release blocked funds" in new Fixture {
    val request = WalletActor.BlockFundsInMultisign(Seq(keyPair, otherKeyPair), 1.BTC)
    instance ! request
    val reply = expectMsgType[WalletActor.FundsBlocked]
    instance ! WalletActor.ReleaseFunds(reply.tx)
    eventually {
      wallet.balance() should be(initialFunds)
    }
  }

  trait Fixture {
    val keyPair = new KeyPair
    val otherKeyPair = new KeyPair
    val wallet = createWallet(keyPair, 10.BTC)
    val initialFunds = wallet.balance()
    val instance = system.actorOf(Props(new WalletActor(wallet)))
  }
}
