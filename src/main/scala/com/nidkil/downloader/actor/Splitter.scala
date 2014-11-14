package com.nidkil.downloader.actor

import com.nidkil.downloader.datatypes.Download
import com.nidkil.downloader.io.DownloadProvider
import com.nidkil.downloader.splitter.DefaultSplitter
import com.nidkil.downloader.splitter.DefaultSplitter.ratioMinMaxStrategy
import Controller.DownloadingStart
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import com.nidkil.downloader.akka.extension.Settings

object Splitter {
  case class Split(download: Download)  
}

class Splitter(monitor: ActorRef) extends Actor with ActorLogging {

  import Controller._
  import Splitter._
  import DefaultSplitter._
  
  def receive = {
    case split: Split => {
      log.info(s"Received Split [${split.download}]")

      try {
        val provider = new DownloadProvider()
        val rfi = provider.remoteFileInfo(split.download.url)
  
        log.info(s"Remote file info [$rfi]")
        
        val settings = Settings(context.system) 
        val splitter = settings.splitter
        val strategy = settings.strategy
        val chunks = splitter.split(rfi, split.download.resumeDownload, split.download.workDir, strategy)
        
        sender ! DownloadingStart(split.download, chunks, rfi)
      } catch {
        case e: IllegalStateException => sender ! DownloadFailed(e)
      } 
    }
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }

}