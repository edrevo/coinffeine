package com.coinffeine.server

import java.net.BindException
import java.util.Currency
import scala.concurrent.duration._

import akka.actor._
import akka.actor.SupervisorStrategy.{Restart, Stop}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.broker.BrokerActor
import com.coinffeine.broker.BrokerActor.BrokeringStart
import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.protocol.gateway.MessageGateway.{Bind, BindingError, BoundTo}
import com.coinffeine.common.protocol.messages.brokerage.Market
import com.coinffeine.common.system.ActorSystemBootstrap
import com.coinffeine.server.BrokerSupervisorActor.InitializedBroker

class BrokerSupervisorActor(
    tradedCurrencies: Set[Currency],
    gatewayProps: Props,
    brokerProps: Props) extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _: BindException => Stop
      case _ => Restart
    }

  val receive: Receive = {
    case ActorSystemBootstrap.Start(args) =>
      val cli = CommandLine.fromArgList(args)
      val serverInfo = new PeerInfo(BrokerSupervisorActor.ListenAddress, cli.port)
      context.become(supervising(sender, startMessageGateway(serverInfo)))
  }

  private def supervising(listener: ActorRef, gateway: ActorRef): Receive = {
    case BindingError(_) | Terminated(`gateway`) =>
      context.stop(self)

    case BoundTo(_) =>
      tradedCurrencies.foreach { currency =>
        context.actorOf(brokerProps) ! BrokeringStart(Market(currency), gateway)
      }
      listener ! InitializedBroker
  }

  private def startMessageGateway(serverInfo: PeerInfo): ActorRef = {
    val gateway = context.actorOf(gatewayProps)
    context.watch(gateway)
    gateway ! Bind(serverInfo)
    gateway
  }
}

object BrokerSupervisorActor {
  val ListenAddress = "localhost"

  case object InitializedBroker

  trait Component {
    this: BrokerActor.Component with MessageGateway.Component =>

    def brokerSupervisorProps(tradedCurrencies: Set[Currency]): Props =
      Props(new BrokerSupervisorActor(tradedCurrencies, messageGatewayProps, brokerActorProps))
  }
}
