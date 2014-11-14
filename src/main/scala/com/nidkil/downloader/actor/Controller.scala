package com.nidkil.downloader.actor

import java.io.File
import java.net.URL
import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.datatypes.Download
import com.nidkil.downloader.datatypes.RemoteFileInfo
import com.nidkil.downloader.event.EventType.MonitorChunks
import com.nidkil.downloader.event.EventTypeSender
import com.nidkil.downloader.utils.Checksum
import com.nidkil.downloader.utils.UrlUtils
import Merger.MergeChunks
import Splitter.Split
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.Broadcast
import akka.routing.FromConfig
import com.nidkil.downloader.akka.extension.Settings
import org.apache.commons.io.FileUtils
import akka.actor.ActorNotFound
import com.nidkil.downloader.manager.State
import com.nidkil.downloader.utils.DownloaderUtils

object Controller {
  case class Startup(shutdownReaper: ActorRef)
  case class DownloadNew(url: URL, checksum: String = null)
  case class DownloadingStart(download: Download, chunks: LinkedHashSet[Chunk], rfi: RemoteFileInfo)
  case class DownloadingCompleted(download: Download)
  case class DownloadCompleted(download: Download)
  case class DownloadFailed(e: Exception)
}

//TODO remove defaults (nulls)
class Controller(master: ActorRef = null, masterMonitor: ActorRef = null) extends Actor with ActorLogging {

  import Controller._
  import DownloadWorker._
  import Merger._
  import Reaper._
  import Splitter._
  import State._

  var shutdownReaper: ActorRef = null
  val settings = Settings(context.system)
  val downloadDir = new File(settings.directory)

  def receive = {
    case start: Startup => {
      log.debug("Received Start")

      shutdownReaper = start.shutdownReaper
    }
    case downloadNew: DownloadNew =>
      log.debug(s"Received DownloadNew [${downloadNew.url.toString}]")

      val destFile = new File(downloadDir, UrlUtils.extractFilename(downloadNew.url))
      val download = destFile.exists match {
        case true if settings.forceDownload => {
          log.info(s"Destination file exists, skipping download [${destFile}]")
          FileUtils.forceDelete(destFile)
          true
        }
        case true => {
          log.info(s"Destination file exists, forcing download [${destFile}]")
          false
        }
        case false => true
      }

      if (download) {
        val id = Checksum.calculate(downloadNew.toString)
        val workDir = new File(downloadDir, id)

        createExecutionContext(new Download(id, downloadNew.url, destFile, workDir, downloadNew.checksum, settings.forceDownload, settings.resumeDownload, State.NONE))
      }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

  // Start separate anonymous download actor context to handle download to ensure all
  // variables are limited to the context of the download
  //TODO add actor to reaper watch?
  def createExecutionContext(download: Download) {
    val localShutdownReaper = shutdownReaper

    context.actorOf(Props(new Actor() with EventTypeSender {
      log.info(s"Started separate actor context to handle download [$download]")

      localShutdownReaper ! WatchMe(self)

      val monitor = if (masterMonitor == null) context.actorOf(Props[Monitor], "monitor") else masterMonitor
      val splitter = context.actorOf(Props(new Splitter(monitor)), "splitter")
      val cleaner = context.actorOf(Props(new Cleaner(context.self, monitor)), "cleaner")
      val validator = context.actorOf(Props(new Validator(cleaner, monitor)), "validate")
      val merger = context.actorOf(Props(new Merger(validator, monitor)), "merger")

      var remoteFileInfo: RemoteFileInfo = null
      var chunks: LinkedHashSet[Chunk] = null

      splitter ! Split(download)

      def sendEvent[T](event: T): Unit = {
        monitor ! event
      }
      
      def gracefulStop: Unit = {
        splitter ! PoisonPill
        cleaner ! PoisonPill
        validator ! PoisonPill
        merger ! PoisonPill

        // Give actors time to close before stopping router
        Thread.sleep(1000)

        // Make sure we stop the separate actor context, so that we do not
        // drain system resources
        context.stop(self)
      }

      def receive = {
        case start: DownloadingStart =>
          log.info(s"Received DownloadingStart [${start.download}][${start.chunks}]")

          chunks = start.chunks
          remoteFileInfo = start.rfi

          DownloaderUtils.writeDebugInfo(download, chunks, remoteFileInfo)

          sendEvent(MonitorChunks(download, chunks))

          for (c <- start.chunks) master ! ChunkDownload(c)
        case dc: DownloadingCompleted =>
          log.info(s"Received DownloadingCompleted [${dc.download}][$download]")

          merger.tell(MergeChunks(download, chunks, remoteFileInfo), self)
        case dc: DownloadCompleted =>
          log.info(s"Received DownloadCompleted [message=${dc.download}][actor=$download]")

          gracefulStop
        case df: DownloadFailed =>
          log.error(s"Download failed: ${df.e.getMessage} [actor=$download]", df.e)

          gracefulStop
        case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
      }
    }))
  }

}