package com.nidkil.downloader.actor

import scala.collection.mutable.ArrayBuffer
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.actor.ActorLogging

object Reaper {
  case class WatchMe(actorRef: ActorRef)
}

abstract class Reaper extends Actor with ActorLogging {

  import Reaper._

  val watched = ArrayBuffer.empty[ActorRef]

  def allSoulsReaped(): Unit

  final def receive = {
    case WatchMe(actorRef) =>
      context.watch(actorRef)
      watched += actorRef
      log.info(s"Received WatchMe [watchCnt=${watched.size}, $actorRef]")
    case Terminated(actorRef) =>
      watched -= actorRef
      log.info(s"Received Terminated [watchCnt=${watched.size}, $actorRef]")
      if (watched.isEmpty) allSoulsReaped()
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }

}