package com.nidkil.downloader.actor

import scala.concurrent.Future
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorNotFound
import scala.util.{Failure, Success}

class ShutdownReaper(controller: ActorRef = null) extends Reaper with ActorLogging {

  def allSoulsReaped() {
    log.info(s"All souls reaped, shutting system down")

    if (controller != null) context.stop(controller)

    context.system.shutdown()
  }

}