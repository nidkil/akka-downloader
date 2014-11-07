package com.nidkil.downloader.event

import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import akka.actor.Actor
import akka.actor.ActorLogging
import com.nidkil.downloader.datatypes.Download

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

trait MonitorEventType extends EventTypeReceiver with ActorLogging { this: Actor =>
  
  import EventType._

  var download: Download = null
  var chunks: LinkedHashSet[Chunk] = null
  var chunksCompleted = 0;
  var chunkCount = -1;

  def sendDownloadCompleted
  
  def eventTypeReceive: Receive = {
    case MonitorChunks(download, chunks) => {
      log.info(s"Received MonitorChunks [download=$download][chunks=$chunks]")
      this.download = download
      this.chunks = chunks
      chunkCount = chunks.size
    }
    case ChunkCompleted(chunk) => { 
      log.info(s"Received ChunkCompleted [chunkCount=$chunkCount, chunksCompleted=${chunksCompleted + 1}, $chunk")
      
      chunksCompleted += 1
      
      if (chunkCount == chunksCompleted) sendDownloadCompleted
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }
}