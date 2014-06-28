package com.coinffeine.client.exchange

import akka.actor.ActorRef

import com.coinffeine.common.bitcoin.ImmutableTransaction
import com.coinffeine.common.bitcoin.peers.PeerActor.TransactionPublished

/** This actor is in charge of broadcasting the appropriate transactions for an exchange, whether
  * the exchange ends successfully or not.
  */
object ExchangeTransactionBroadcastActor {

  /** Sets the refund transaction to be used. This transaction will be broadcast as soon as its
    * timelock expires if there are no better alternatives (like broadcasting the successful
    * exchange transaction)
    */
  case class SetRefund(refund: ImmutableTransaction)

  /** Sets the micropayment actor, which will be queried for transactions which are deemed better
    * than the refund transaction.
    */
  case class SetMicropaymentActor(micropaymentRef: ActorRef)

  /** A request for the actor to finish the exchange and broadcast the best possible transaction */
  case object FinishExchange

  /** A reply to FinishExchange indicating that the exchange could be finished by broadcasting a
    * transaction. This message can also be sent once the micropayment actor has been set if the
    * exchange has been forcefully closed due to the risk of having the refund exchange be valid.
    */
  case class ExchangeFinished(publishedTransaction: TransactionPublished)

  /** A reply to FinishExchange indicating that the broadcast of the best transaction was not
    * performed due to an error.
    */
  case class ExchangeFinishFailure(cause: Throwable)
}
