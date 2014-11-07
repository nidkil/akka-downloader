package com.nidkil.downloader.actor

import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.datatypes.Download
import akka.actor.Actor
import akka.actor.ActorLogging
import com.nidkil.downloader.validator.ChecksumValidator
import com.nidkil.downloader.datatypes.RemoteFileInfo
import com.nidkil.downloader.validator.FileSizeValidator
import java.io.File
import akka.actor.ActorRef

object Validator {
  case class Validate(download: Download, rfi: RemoteFileInfo, tempFile: File)
}

//TODO Join Merger, Validator and Cleaner into a single actor
class Validator(cleaner: ActorRef, monitor: ActorRef) extends Actor with ActorLogging {

  import Cleaner._
  import Validator._

  def receive = {
    case validate: Validate => {
      log.info(s"Received Validate [${validate.download}]")
      
      if (validate.download.checksum == null) {
        val validator = new FileSizeValidator(validate.rfi.fileSize)
        validator.validate(validate.tempFile)
      } else {
        val validator = new ChecksumValidator(validate.download.checksum)
        validator.validate(validate.tempFile)
      }
      
      cleaner.tell(Clean(validate.download, validate.tempFile), sender)
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}