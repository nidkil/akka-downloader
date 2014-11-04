package com.nidkil.downloader.actor

import java.io.File
import com.nidkil.downloader.cleaner.DefaultCleaner
import com.nidkil.downloader.datatypes.Download
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef

object Cleaner {
  case class Clean(download: Download, tempFile: File)
}

class Cleaner(controller: ActorRef, monitor: ActorRef) extends Actor with ActorLogging {

  import Cleaner._
  import Controller._

  def receive = {
    case clean: Clean => {
      log.info(s"Received Clean [${clean.download}][tempFile=${clean.tempFile}]")
      
      val cleaner = new DefaultCleaner()
      cleaner.clean(clean.download, clean.tempFile)
      
      controller ! Completed(clean.download)
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}