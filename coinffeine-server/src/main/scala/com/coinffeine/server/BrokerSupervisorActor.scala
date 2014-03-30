package com.coinffeine.server

import java.net.BindException
import scala.concurrent.duration._

import akka.actor._
import akka.actor.SupervisorStrategy.{Restart, Stop}
import com.googlecode.protobuf.pro.duplex.PeerInfo

import com.coinffeine.broker.BrokerActor
import com.coinffeine.common.currency.CurrencyCode._
import com.coinffeine.common.protocol.gateway.MessageGateway

class BrokerSupervisorActor(gatewayProps: Props, brokerProps: ActorRef => Seq[Props]) extends Actor {

  private val gateway = context.actorOf(gatewayProps)
  private val brokers = brokerProps(gateway).map(props => context.actorOf(props))

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _: BindException => Stop
      case _ => Restart
    }

  override def preStart(): Unit = context.watch(gateway)

  val receive: Receive = {
    case Terminated(`gateway`) => self ! PoisonPill
  }
}

object BrokerSupervisorActor {
  val ListenAddress = "localhost"
  val TradedCurrencies = Set(EUR, USD).map(_.currency)

  trait Component {
    this: BrokerActor.Component with MessageGateway.Component =>

    def brokerSupervisorProps(port: Int): Props = {
      val serverInfo = new PeerInfo(ListenAddress, port)
      Props(new BrokerSupervisorActor(messageGatewayProps(serverInfo), brokerProps))
    }

    private def brokerProps(gateway: ActorRef): Seq[Props] = for {
      currency <- TradedCurrencies.toSeq
    } yield brokerActorProps(currency, gateway, ???) // FIXME: inject an actual handshake arbiter
  }
}
