package com.coinffeine.common.system

import scala.util.control.NonFatal

import akka.actor.{ActorSystem, Props}

import com.coinffeine.common.system.ActorSystemBootstrap.Start

/** Bootstrap of an actor system whose supervisor actor is configured from CLI arguments and
  * whose termination stops the application.
  */
trait ActorSystemBootstrap {

  protected val supervisorProps: Props

  def main(commandLine: Array[String]): Unit = {
    val system = ActorSystem("Main")
    try {
      val supervisor = system.actorOf(supervisorProps, "supervisor")
      system.actorOf(Props(classOf[akka.Main.Terminator], supervisor), "terminator")
      supervisor ! Start(commandLine)
    } catch {
      case NonFatal(ex) =>
        system.shutdown()
        throw ex
    }
  }
}

object ActorSystemBootstrap {
  case class Start(commandLine: Array[String])
}
