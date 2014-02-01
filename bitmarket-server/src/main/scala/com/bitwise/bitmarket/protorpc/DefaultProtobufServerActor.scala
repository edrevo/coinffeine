package com.bitwise.bitmarket.protorpc

import java.util.Currency
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Try, Failure, Success}

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import com.google.protobuf.{RpcCallback, RpcController}
import com.googlecode.protobuf.pro.duplex.{ClientRpcController, PeerInfo}
import com.googlecode.protobuf.pro.duplex.execute.ServerRpcController

import com.bitwise.bitmarket.broker.BrokerActor._
import com.bitwise.bitmarket.common.PeerConnection
import com.bitwise.bitmarket.common.protocol._
import com.bitwise.bitmarket.common.protocol.protobuf.{BitmarketProtobuf => proto}
import com.bitwise.bitmarket.common.protocol.protobuf.ProtobufConversions._
import com.bitwise.bitmarket.common.protorpc.PeerServer

private class DefaultProtobufServerActor(
    serverInfo: PeerInfo,
    brokers: Map[Currency, ActorRef],
    brokerTimeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging {

  import DefaultProtobufServerActor._
  import context.dispatcher

  private val selfRef = context.self
  private val service = new proto.BrokerService.Interface() {

    override def requestQuote(
        controller: RpcController,
        request: proto.QuoteRequest,
        done: RpcCallback[proto.QuoteResponse]) {
      Try(Currency.getInstance(request.getCurrency)) match {
        case Success(currency) if brokers.contains(currency) =>
          val spreadPromise = Promise[Quote]()
          spreadPromise.future onComplete {
            case Success(quote) =>
              val response = proto.QuoteResponse.newBuilder
                .setResult(proto.QuoteResponse.Result.FOUND_QUOTE)
                .setQuote(toProtobuf(quote))
                .build
              done.run(response)

            case Failure(ex) =>
              done.run(proto.QuoteResponse.newBuilder
                .setResult(proto.QuoteResponse.Result.TIMEOUT)
                .build)
          }
          selfRef ! RequestQuoteCallback(currency, spreadPromise)

        case Success(currency) =>
          log.error(s"Dropping quote request for $currency: no broker available")
          done.run(proto.QuoteResponse.newBuilder
            .setResult(proto.QuoteResponse.Result.CURRENCY_NOT_TRADED)
            .build)

        case Failure(_) =>
          log.error(s"Dropping quote request for ${request.getCurrency}: unknown currency")
          done.run(proto.QuoteResponse.newBuilder
            .setResult(proto.QuoteResponse.Result.UNKNOWN_CURRENCY)
            .build)
      }
    }

    override def placeOrder(
        controller: RpcController, order: proto.Order, done: RpcCallback[proto.OrderResponse]) {
      val isTraded = brokers.keys.exists(c => c.toString == order.getPrice.getCurrency)
      if (isTraded) {
        selfRef ! PlaceOrderCallback(fromProtobuf(order, senderConnection(controller)))
        done.run(SuccessfulResult)
      } else done.run(CurrencyNotTradedResult)
    }
  }

  private def senderConnection(controller: RpcController): PeerConnection = {
    val peerInfo = controller match {
      case serverController: ServerRpcController => serverController.getRpcClient.getPeerInfo
      case clientController: ClientRpcController => clientController.getRpcClient.getPeerInfo
    }
    PeerConnection(peerInfo.getHostName, peerInfo.getPort)
  }

  private val server: PeerServer = {
    new PeerServer(serverInfo, proto.BrokerService.newReflectiveService(service))
  }

  override def preStart() {
    val starting = server.start()
    starting.await()
    if (!starting.isSuccess) {
      server.shutdown()
      throw starting.cause()
    }
  }

  override def postStop() {
    server.shutdown()
  }

  def receive: Receive = {

    case RequestQuoteCallback(currency, quotePromise) =>
      implicit val quoteTimeout = Timeout(brokerTimeout)
      val broker = brokers(currency)
      (broker ? QuoteRequest(currency)).mapTo[QuoteResponse].map(_.quote) onComplete quotePromise.complete

    case PlaceOrderCallback(order) =>
      brokers.get(order.price.currency).foreach { broker =>
        broker ! OrderPlacement(order)
      }
  }
}

object DefaultProtobufServerActor {

  trait Component extends ProtobufServerActor.Component {
    override def protobufServerActorProps(port: Int, brokers: Map[Currency, ActorRef]) =
      Props(new DefaultProtobufServerActor(new PeerInfo("localhost", port), brokers))
  }

  private case class RequestQuoteCallback(currency: Currency, spreadPromise: Promise[Quote])
  private case class PlaceOrderCallback(order: Order)

  private val SuccessfulResult = proto.OrderResponse.newBuilder
    .setResult(proto.OrderResponse.Result.SUCCESS)
    .build
  private val CurrencyNotTradedResult = proto.OrderResponse.newBuilder
    .setResult(proto.OrderResponse.Result.CURRENCY_NOT_TRADED)
    .build
}
