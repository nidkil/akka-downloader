package com.nidkil.downloader.actor

import scala.concurrent.Future
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Status.Failure
import akka.actor.Status.Success
import akka.actor.ActorNotFound

class ShutdownReaper(controller: ActorRef = null) extends Reaper with ActorLogging {

  def allSoulsReaped() {
    log.info(s"All souls reaped, shutting system down")

    try {
      import scala.concurrent.duration._
      val test = scala.concurrent.Await.result(context.actorSelection("../splitter").resolveOne()(akka.util.Timeout.intToTimeout(1000)), 10.seconds)
      println(s" ******* $test")
    } catch {
      case e: ActorNotFound => println(s" ******* NOT FOUND")
    }

    import context.dispatcher
    val future = Future {
      context.actorSelection("../controllerRouter").resolveOne()(akka.util.Timeout.intToTimeout(1000))
    } 
    
    future.onComplete {
      case result => println(s"success=$result")
      case failure => println(s"failure=$failure")
    }

    if (controller != null) context.stop(controller)

    context.system.shutdown()
  }

}