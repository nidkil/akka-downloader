package com.nidkil.downloader.actor

import java.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import com.nidkil.downloader.datatypes.Chunk
import com.nidkil.downloader.event.EventType
import com.nidkil.downloader.event.EventType.ChunkCompleted
import com.nidkil.downloader.event.EventTypeSender
import com.nidkil.downloader.io.DownloadProvider
import com.nidkil.downloader.io.ProtocolDriverException
import com.nidkil.downloader.manager.State

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.actorRef2Scala

object Downloader {
  case class ChunkDownload(chunk: Chunk)
}

class Downloader(monitor: ActorRef) extends Actor with EventTypeSender with ActorLogging {

  import Downloader._
  import EventType._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  def sendEvent[T](event: T): Unit = {
    monitor ! event
  }

  val random = new Random(System.currentTimeMillis)

  def receive = {
    case download: ChunkDownload => {
      log.info(s"Received ChunkDownload [${download.chunk}]")

      download.chunk.state match {
        case State.DOWNLOADED => {
          log.info(s"Chunk is complete, skipping download [${download.chunk}]")
          sendEvent(ChunkCompleted(download.chunk))
        }
        case _ => {
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
              if (e.getMessage.contains("Connection refused")) {
                // If connection refused is given back the server limits the number of connections,
                // so we have to throttle the downloads to this server. Delay (=throttle) the actor
                // for a random number of seconds
                val throttle = random.nextInt(10000)
                log.warning(s"Received connection refused from server, enabling connection throttling and will retry [retry=$throttle ms, ${download.chunk}]")
                // Schedule sending the request execution
                context.system.scheduler.scheduleOnce(throttle milliseconds, self, download)
              } else {
                log.error(s"Exception occured downloading chunk that cannot be handled, aborting download [${e.getMessage}]")
              }
            }
          }
        }
      }
    }
    case x => log.warning(s"Unknown message received by ${self.path} [${x.getClass}, value=$x]")
  }

}