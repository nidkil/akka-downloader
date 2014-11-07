package com.nidkil.downloader.actor

import scala.collection.mutable.LinkedHashSet
import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.datatypes.Download
import akka.actor.Actor
import akka.actor.ActorLogging
import com.nidkil.downloader.merger.DefaultMerger
import akka.actor.ActorRef
import com.nidkil.downloader.datatypes.RemoteFileInfo

object Merger {
  case class MergeChunks(download: Download, chunks: LinkedHashSet[Chunk], rfi: RemoteFileInfo)
}

class Merger(validator: ActorRef, monitor: ActorRef) extends Actor with ActorLogging {

  import Merger._
  import Validator._

  def receive = {
    case merge: MergeChunks => {
      log.info(s"Received MergeChunks [${merge.download}][chunks=${merge.chunks.size}]")
      
      val merger = new DefaultMerger()
      merger.merge(merge.download, merge.chunks)
      
      validator.tell(Validate(merge.download, merge.rfi, merger.tempFile), sender)
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}