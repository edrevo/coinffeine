package com.bitwise.bitmarket.server

import java.net.BindException
import java.util.Currency
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.actor.SupervisorStrategy.{Restart, Stop}

import com.bitwise.bitmarket.broker.BrokerActor
import com.bitwise.bitmarket.common.currency.CurrencyCode._
import com.bitwise.bitmarket.protorpc.ProtobufServerActor
import com.bitwise.bitmarket.system.SupervisorComponent
import com.bitwise.bitmarket.CommandLine

class ServerActor(
    brokerProps: Map[Currency, Props],
    protobufServerProps: Map[Currency, ActorRef] => Props
  ) extends Actor {

  val brokers: Map[Currency, ActorRef] = for {
    (currency, props) <- brokerProps
  } yield currency -> context.actorOf(props)

  val protobufServer = context.actorOf(protobufServerProps(brokers))

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10 seconds) {
      case _: BindException => Stop
      case _ => Restart
    }

  override def preStart() {
    context.watch(protobufServer)
  }

  def receive: Actor.Receive = {
    case Terminated(`protobufServer`) => self ! PoisonPill
  }
}

object ServerActor {
  val ListenAddress = "localhost"
  val TradedCurrencies = Set(EUR, USD).map(_.currency)

  trait Component extends SupervisorComponent {
    this: BrokerActor.Component with ProtobufServerActor.Component =>

    override def supervisorProps(args: Array[String]) = {
      val cli = CommandLine.fromArgList(args)
      val brokerProps = TradedCurrencies.map(currency =>
        currency -> brokerActorProps(currency)
      ).toMap
      Props(new ServerActor(brokerProps, brokers => protobufServerActorProps(cli.port, brokers)))
    }
  }
}
