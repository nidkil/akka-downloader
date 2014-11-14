package com.nidkil.downloader.actor

import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.datatypes.Download
import com.nidkil.downloader.event.MonitorEventType
import com.nidkil.downloader.event.MonitorDownload
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef

object Monitor {
  case class REMOVE(download: Download, chunks: LinkedHashSet[Chunk])  
}

class Monitor extends Actor with MonitorEventType with ActorLogging {

  import Controller._
  import Monitor._

  def sendDownloadCompleted(monitor: MonitorDownload): Unit = {
    log.info(s"Send DownloadCompleted [${monitor.download}][${monitor.chunks}]")
    monitor.controller ! DownloadingCompleted(monitor.download)
  }

  def monitorReceive: Receive = {
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }
  
   def receive = eventTypeReceive orElse monitorReceive

}