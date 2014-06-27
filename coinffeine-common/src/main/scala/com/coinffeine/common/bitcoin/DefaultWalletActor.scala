package com.coinffeine.common.bitcoin

import scala.util.control.NonFatal

import akka.actor.{Actor, ActorLogging, Props}

import com.coinffeine.common.bitcoin.Implicits._

class DefaultWalletActor(wallet: Wallet) extends Actor with ActorLogging {

  override val receive: Receive = {
    case req @ WalletActor.BlockFundsInMultisign(signatures, amount) =>
      try {
        val tx = wallet.blockMultisignFunds(signatures, amount)
        sender ! WalletActor.FundsBlocked(req, tx)
      } catch {
        case NonFatal(ex) => sender ! WalletActor.FundsBlockingError(req, ex)
      }
    case WalletActor.ReleaseFunds(tx) =>
      wallet.releaseFunds(tx)
  }
}

object DefaultWalletActor {

  trait Component extends WalletActor.Component {
    override def walletActorProps(wallet: Wallet) = Props(new DefaultWalletActor(wallet))
  }
}
