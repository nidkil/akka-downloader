package com.nidkil.downloader.actor

import akka.actor.Terminated
import akka.actor.ActorLogging
import com.nidkil.downloader.protocol.MasterWorkerProtocol
import akka.actor.Actor
import akka.actor.ActorRef

/**
 * Adapted from https://github.com/derekwyatt/akka-worker-pull
 */
class Master extends Actor with ActorLogging {

  import MasterWorkerProtocol._
  import scala.collection.mutable.{ Map, Queue }

  // Holds known workers and what they may be working on
  val workers = Map.empty[ActorRef, Option[Tuple2[ActorRef, Any]]]
  // Holds the incoming list of work to be done as well
  // as the memory of who asked for it
  val workQueue = Queue.empty[Tuple2[ActorRef, Any]]
  
  // Notifies workers that there's work available, provided they're
  // not already working on something
  def notifyWorkers(): Unit = {
    if (!workQueue.isEmpty) {
      workers.foreach {
        case (worker, m) if (m.isEmpty) => worker ! WorkIsReady
        case _ =>
      }
    }
  }

  def receive = {
    // Worker is alive. Add him to the list, watch him for
    // death, and let him know if there's work to be done
    case WorkerCreated(worker) => {
      log.info(s"Worker created: $worker")
      context.watch(worker)
      workers += (worker -> None)
      notifyWorkers()
    }
    // A worker wants work. If it is know, it is currently not doing anything, 
    // and we have something to do, give it to the worker
    case WorkerRequestsWork(worker) => {
      log.info(s"Worker requests work: $worker")
      if (workers.contains(worker)) {
        if (workQueue.isEmpty)
          worker ! NoWorkToBeDone
        else if (workers(worker) == None) {
          val (workSender, work) = workQueue.dequeue()
          workers += (worker -> Some(workSender -> work))
          // Use the special form of 'tell' that lets us supply the sender
          worker.tell(WorkToBeDone(work), workSender)
        }
      }
    }
    // Worker has completed its work and we can clear it out
    case WorkIsDone(worker) => {
      if (!workers.contains(worker)) {
        log.error(s"Error: received completed work event, but worker unknown [$worker]")
      } else {
        workers += (worker -> None)
      }
    }
    // A worker died. If it was doing anything then we need
    // to give it to another worker, so add it back to the
    // master and let things progress as usual
    case Terminated(worker) => {
      if (workers.contains(worker) && workers(worker) != None) {
        log.error(s"Error: $worker died while processing [${workers(worker)}]")
        // Send the work it was doing back to ourselves for processing
        val (workSender, work) = workers(worker).get
        self.tell(work, workSender)
      }
      workers -= worker
    }
    // Anything other than our own protocol is "work to be done"
    case work => {
      log.info(s"Queueing: $work")
      workQueue.enqueue(sender -> work)
      notifyWorkers()
    }
  }

}