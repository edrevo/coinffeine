package com.coinffeine.common

/** This package contains utilities for bootstraping an actor system given a supervisor actor
  * and gracefully shutdown when it terminates.
  *
  * Example use:
  *
  * {{{
  *   object Main extends ActorSystemBootstrap with TopmostActorComponent with ...
  * }}}
  *
  * Where ``TopmostActorComponent`` extends ``SupervisorComponent``, thus providing the behaviour
  * for the whole system (creation of the actor hierarchy, top-level surpervision).
  *
  * @see [[com.coinffeine.common.system.ActorSystemBootstrap]]
  */
package object system
