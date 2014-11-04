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
import Downloader.DownloadChunk
import Merger.MergeChunks
import Reaper.WatchMe
import Splitter.Split
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.Broadcast
import akka.routing.RoundRobinPool
import com.nidkil.downloader.event.EventType
import java.io.PrintWriter
import org.apache.commons.io.FileUtils

object Controller {
  case class Startup(shutdownReaper: ActorRef)
  case class NewDownload(url: URL, checksum: String = null)
  case class StartDownload(download: Download, chunks: LinkedHashSet[Chunk], rfi: RemoteFileInfo)
  case class DownloadCompleted(download: Download)
  case class Completed(download: Download)
}

class Controller extends Actor with EventTypeSender with ActorLogging {

  import Controller._
  import Downloader._
  import EventType._
  import Merger._
  import Reaper._
  import Splitter._

  var monitor: ActorRef = null
  var splitter: ActorRef = null
  var cleaner: ActorRef = null
  var validator: ActorRef = null
  var merger: ActorRef = null
  var downloaderRouter: ActorRef = null
  var shutdownReaper: ActorRef = null

  val downloadDir = new File(curDir, "download")

  var download: Download = null
  var remoteFileInfo: RemoteFileInfo = null
  var chunks: LinkedHashSet[Chunk] = null

  //TODO make configurable
  def curDir = new java.io.File(".").getCanonicalPath

  def sendEvent[T](event: T): Unit = {
    monitor ! event
  }

  def writeDebugInfo(): Unit = {
    if (!download.workDir.exists) FileUtils.forceMkdir(download.workDir)
    val out = new PrintWriter(new File(download.workDir, "debug.info"), "UTF-8")
    try {
      out.println(download)
      out.println(remoteFileInfo)
      out.println(chunks)
    } finally {
      out.close
    }
  }

  def receive = {
    case start: Startup => {
      log.info("Received Start")

      shutdownReaper = start.shutdownReaper

      //TODO Move to startup receive
      monitor = context.actorOf(Props(new Monitor(context.self)), "monitor")
      splitter = context.actorOf(Props(new Splitter(monitor)), "splitter")
      cleaner = context.actorOf(Props(new Cleaner(context.self, monitor)), "cleaner")
      validator = context.actorOf(Props(new Validator(cleaner, monitor)), "validate")
      merger = context.actorOf(Props(new Merger(validator, monitor)), "merger")
      downloaderRouter = context.actorOf(RoundRobinPool(8).props(Props(new Downloader(monitor))), "downloader")

      shutdownReaper ! WatchMe(splitter)
      shutdownReaper ! WatchMe(cleaner)
      shutdownReaper ! WatchMe(validator)
      shutdownReaper ! WatchMe(merger)
      shutdownReaper ! WatchMe(monitor)
      shutdownReaper ! WatchMe(downloaderRouter)
    }
    case dc: DownloadCompleted => {
      log.info(s"Received DownloadCompleted [${dc.download}][$download]")

      merger ! MergeChunks(download, chunks, remoteFileInfo)
    }
    case c: Completed => {
      log.info(s"Received Completed [${c.download}][$download]")

      // Stop all actors that belong to this controller
      splitter ! PoisonPill
      cleaner ! PoisonPill
      validator ! PoisonPill
      merger ! PoisonPill
      monitor ! PoisonPill
      downloaderRouter ! Broadcast(PoisonPill)
    }
    case newDownload: NewDownload => {
      log.info(s"Received NewDownload [${newDownload.url.toString}]")

      val id = Checksum.calculate(newDownload.url.toString)
      val destFile = new File(downloadDir, UrlUtils.extractFilename(newDownload.url))
      val workDir = new File(downloadDir, id)
      val download = new Download(id, newDownload.url, destFile, workDir, newDownload.checksum)

      splitter ! Split(download)
    }
    case start: StartDownload => {
      log.info(s"Received StartDownload [${start.download}]")

      download = start.download
      remoteFileInfo = start.rfi
      chunks = start.chunks

      writeDebugInfo
      sendEvent(MonitorChunks(download, chunks))

      for (c <- start.chunks) downloaderRouter ! DownloadChunk(c)
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}