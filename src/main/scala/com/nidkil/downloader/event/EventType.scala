package com.nidkil.downloader.event

import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import akka.actor.Actor
import akka.actor.ActorLogging
import com.nidkil.downloader.datatypes.Download
import akka.actor.ActorRef
import java.net.URL

trait EventTypeSender {
  def sendEvent[T](event: T): Unit
}

trait EventTypeReceiver {
  def eventTypeReceive: Actor.Receive
}

object EventType {
  case class MonitorChunks(download: Download, chunks: LinkedHashSet[Chunk])
  case class ChunkCompleted(chunk: Chunk)
}

case class MonitorDownload(controller: ActorRef, download: Download, chunks: LinkedHashSet[Chunk], var chunkCount: Int, var chunksCompleted: Int)

trait MonitorEventType extends EventTypeReceiver with ActorLogging { this: Actor =>
  
  import EventType._
  import scala.collection.mutable.Map

  var downloads = Map.empty[URL, MonitorDownload]

  def sendDownloadCompleted(monitor: MonitorDownload): Unit
  
  def eventTypeReceive: Receive = {
    case MonitorChunks(download, chunks) => {
      log.info(s"Received MonitorChunks [sender=$sender][download=$download][chunks=$chunks]")
      
      if(downloads.contains(download.url)) throw new Exception(s"Download exists [$download]")
      
      downloads += download.url -> MonitorDownload(sender, download, chunks, chunks.size, 0)
    }
    case ChunkCompleted(chunk) => {
      if(!downloads.contains(chunk.url)) throw new Exception(s"Unkown download [$chunk]")
      
      val monitor = downloads(chunk.url)
      
      monitor.chunksCompleted += 1
      downloads += monitor.download.url -> monitor
      
      log.info(s"Received ChunkCompleted [chunkCount=${monitor.chunkCount}, chunksCompleted=${monitor.chunksCompleted}, $chunk")
      
      if (monitor.chunkCount == monitor.chunksCompleted) sendDownloadCompleted(monitor)
    }
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }
}