package com.nidkil.downloader.actor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.nidkil.downloader.event.EventType
import com.nidkil.downloader.event.EventTypeSender
import com.nidkil.downloader.io.DownloadProvider
import com.nidkil.downloader.manager.State
import akka.actor.ActorLogging
import akka.actor.ActorPath
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import com.nidkil.downloader.datatypes.Chunk

object DownloadWorker {
  case class ChunkDownload(chunk: Chunk)
}

class DownloadWorker(masterPath: ActorPath, monitor: ActorRef) extends Worker(masterPath) with EventTypeSender with ActorLogging {

  import DownloadWorker._
  import EventType._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  def sendEvent[T](event: T): Unit = {
    monitor ! event
  }

  def doWork(workSender: ActorRef, work: Any): Unit = {
    val download = work.asInstanceOf[ChunkDownload]
    download.chunk.state match {
      case State.DOWNLOADED => {
        log.info(s"Chunk already downloaded, skipping download [${download.chunk}]")
        sendEvent(ChunkCompleted(download.chunk))
        self ! WorkComplete(download.chunk)
      }
      case _ => {
        Future {
          log.info(s"Downloading chunk [${download.chunk}]")
          val provider = new DownloadProvider()
          provider.download(download.chunk)
          sendEvent(ChunkCompleted(download.chunk))
          WorkComplete(download.chunk)
        } pipeTo self
      }
    }
  }

}