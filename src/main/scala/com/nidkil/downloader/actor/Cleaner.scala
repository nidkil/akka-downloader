package com.nidkil.downloader.actor

import java.io.File
import com.nidkil.downloader.cleaner.DefaultCleaner
import com.nidkil.downloader.datatypes.Download
import Controller.DownloadCompleted
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import com.nidkil.downloader.akka.extension.Settings

object Cleaner {
  case class Clean(download: Download, tempFile: File)
}

class Cleaner(controller: ActorRef, monitor: ActorRef) extends Actor with ActorLogging {

  import Cleaner._
  import Controller._

  def receive = {
    case clean: Clean => {
      log.info(s"Received Clean [${clean.download}][tempFile=${clean.tempFile}]")
      
      val settings = Settings(context.system) 
      val cleaner = settings.cleaner
      cleaner.clean(clean.download, clean.tempFile)
      
      sender ! DownloadCompleted(clean.download)
    }
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }

}