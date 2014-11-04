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
      log.info(s"Received WatchMe [watchCnt=${watched.size + 1}, $actorRef]")
      
      context.watch(actorRef)
      
      watched += actorRef
    case Terminated(actorRef) =>
      log.info(s"Received Terminated [watchCnt=${watched.size - 1}, $actorRef]")
      
      watched -= actorRef
      
      if (watched.isEmpty) allSoulsReaped()
  }

}