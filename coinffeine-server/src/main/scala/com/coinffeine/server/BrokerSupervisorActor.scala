package com.coinffeine.server

import java.net.BindException
import java.util.Currency
import scala.concurrent.duration._

import akka.actor._
import akka.actor.SupervisorStrategy.{Restart, Stop}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.broker.BrokerActor
import com.coinffeine.broker.BrokerActor.StartBrokering
import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.protocol.gateway.MessageGateway.{Bind, BindingError}

class BrokerSupervisorActor(
    serverInfo: PeerInfo,
    tradedCurrencies: Set[Currency],
    gatewayProps: Props,
    brokerProps: Props) extends Actor {

  private val gateway = context.actorOf(gatewayProps)
  private val brokers = tradedCurrencies.foreach { currency =>
    context.actorOf(brokerProps) ! StartBrokering(currency, gateway)
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _: BindException => Stop
      case _ => Restart
    }

  override def preStart(): Unit = {
    context.watch(gateway)
    gateway ! Bind(serverInfo)
  }

  val receive: Receive = {
    case BindingError(_) | Terminated(`gateway`) => self ! PoisonPill
  }
}

object BrokerSupervisorActor {
  val ListenAddress = "localhost"

  trait Component {
    this: BrokerActor.Component with MessageGateway.Component =>

    def brokerSupervisorProps(port: Int, tradedCurrencies: Set[Currency]): Props =
      Props(new BrokerSupervisorActor(
        serverInfo = new PeerInfo(ListenAddress, port),
        tradedCurrencies, messageGatewayProps, brokerActorProps))
  }
}
