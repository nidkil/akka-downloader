package com.nidkil.downloader.actor

import akka.actor.ActorLogging
import akka.actor.Actor
import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.io.DownloadProvider
import akka.actor.ActorRef
import com.nidkil.downloader.event.EventTypeSender
import com.nidkil.downloader.event.EventType
import java.io.IOException
import java.util.Random
import com.nidkil.downloader.io.ProtocolDriverException

object Downloader {
  case class DownloadChunk(chunk: Chunk)
}

class Downloader(monitor: ActorRef) extends Actor with EventTypeSender with ActorLogging {

  import Downloader._
  import EventType._

  def sendEvent[T](event: T): Unit = {
    monitor ! event  
  }
  
  val random = new Random(System.currentTimeMillis)
  
  def receive = {
    case download: DownloadChunk => {
      log.info(s"Received DownloadChunk [${download.chunk}]")
      
      val provider = new DownloadProvider()

      try {
        provider.download(download.chunk)        
      
        sendEvent(ChunkCompleted(download.chunk))
      } catch {        
        //TODO Check with monitor to make sure other downloads to the server have been
        // succesfull so that we do not land up getting stuck
        //TODO Is there another way to handle retries? Maybe increasing throttle time on
        //each retry?
        //TODO Solve this in a different way, for example pass back to controller and ask
        // controller to delay giving download request back. This way the actor is freed
        // up to handle requests to other servers
        case e: ProtocolDriverException => {
          if(e.getMessage.contains("Connection refused")) {
            // If connection refused is given back the server limits the number of connections,
            // so we have to throttle the downloads to this server. Delay (=throttle) the actor
            // for a random number of seconds
            val throttle = random.nextInt(10000)
            log.warning(s"Received connection refused from server, enabling connection throttling and will retry [retry=$throttle ms, ${download.chunk}]")
            Thread.sleep(throttle)
            
            self ! download
          } else {
            log.error(s"Exception occured downloading chunk that cannot be handled, aborting downalod [${e.getMessage}]")            
          }
        }
      }
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}