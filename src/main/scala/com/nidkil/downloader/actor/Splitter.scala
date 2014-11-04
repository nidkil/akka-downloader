package com.nidkil.downloader.actor

import com.nidkil.downloader.io.DownloadProvider
import akka.actor.ActorLogging
import com.nidkil.downloader.splitter.DefaultSplitter
import com.nidkil.downloader.datatypes.Download
import akka.actor.Actor
import akka.actor.ActorRef
import com.nidkil.downloader.io.DownloadProvider
import com.nidkil.downloader.datatypes.Download
import com.nidkil.downloader.splitter.DefaultSplitter
import java.io.File

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

      val provider = new DownloadProvider()
      val rfi = provider.remoteFileInfo(split.download.url)

      log.info(s"Remote file info [$rfi]")
      
      val splitter = new DefaultSplitter()
      //TODO Make configurable
      val chunks = splitter.split(rfi, split.download.append, new File(split.download.workDir, split.download.id), ratioMinMaxStrategy)
      
      sender ! StartDownload(split.download, chunks, rfi)
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}