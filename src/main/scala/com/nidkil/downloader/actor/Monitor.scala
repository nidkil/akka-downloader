package com.nidkil.downloader.actor

import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.event.MonitorEventType
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import com.nidkil.downloader.datatypes.Download

object Monitor {
  case class REMOVE(download: Download, chunks: LinkedHashSet[Chunk])  
}

class Monitor(controller: ActorRef) extends Actor with MonitorEventType with ActorLogging {

  import Controller._
  import Monitor._

  def sendComplete: Unit = {
    log.info(s"Send Completed [$download][$chunks]")
    controller ! Completed(download)
  }

  def monitorReceive: Receive = {
    case monitor: REMOVE => {
      log.info(s"Received REMOVE [$monitor]")
      
      download = monitor.download
      chunks = monitor.chunks
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }
  
   def receive = eventTypeReceive orElse monitorReceive

}