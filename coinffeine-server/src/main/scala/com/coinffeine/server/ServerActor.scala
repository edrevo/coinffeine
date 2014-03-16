package com.coinffeine.server

import java.net.BindException
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.actor.SupervisorStrategy.{Restart, Stop}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.CommandLine
import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.currency.CurrencyCode._
import com.coinffeine.common.protocol.gateway.MessageGateway
import com.coinffeine.common.system.SupervisorComponent

class ServerActor(gatewayProps: Props, brokerProps: ActorRef => Seq[Props]) extends Actor {

  private val gateway = context.actorOf(gatewayProps)
  private val brokers = brokerProps(gateway).map(props => context.actorOf(props))

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _: BindException => Stop
      case _ => Restart
    }

  override def preStart() {
    context.watch(gateway)
  }

  def receive: Actor.Receive = {
    case Terminated(`gateway`) => self ! PoisonPill
  }
}

object ServerActor {
  val ListenAddress = "localhost"
  val TradedCurrencies = Set(EUR, USD).map(_.currency)

  trait Component extends SupervisorComponent {
    this: BrokerActor.Component with MessageGateway.Component =>

    override def supervisorProps(args: Array[String]) = {
      val cli = CommandLine.fromArgList(args)
      val serverInfo = new PeerInfo("localhost", cli.port)
      Props(new ServerActor(messageGatewayProps(serverInfo), brokerProps))
    }

    private def brokerProps(gateway: ActorRef): Seq[Props] = for {
      currency <- TradedCurrencies.toSeq
    } yield brokerActorProps(currency, gateway, ???) // FIXME: inject an actual handshake arbiter
  }
}
