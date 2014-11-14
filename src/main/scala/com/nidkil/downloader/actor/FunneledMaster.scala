package com.nidkil.downloader.actor

import scala.collection.mutable.Map
import com.nidkil.downloader.queue.GroupedQueue
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import com.nidkil.downloader.protocol.MasterWorkerProtocol
import akka.actor.PoisonPill

/**
 * Adapted from https://github.com/derekwyatt/akka-worker-pull
 */
class FunneledMaster(lookupGroup: Any => String) extends Actor with ActorLogging {

  import MasterWorkerProtocol._
  import scala.collection.mutable.Map

  // Holds known workers and what they are working on
  val workers = Map.empty[ActorRef, Option[Tuple2[ActorRef, Any]]]
  // Holds the incoming list of work to be done as well as who asked for it
  val workQueues = new GroupedQueue(2)
  
  override def postStop: Unit = {
    workers.foreach {
      case (worker, m) => worker ! PoisonPill
      case _ =>
    }
  }
  
  // Notifies workers that there's work available, provided they're
  // not already working on something
  def notifyWorkers(): Unit = {
    if (!workQueues.isEmpty) {
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
      log.debug(s"Worker requesting work: $worker")
      if (workers.contains(worker)) {
        if (workQueues.isEmpty) {
          log.debug(s"Work queues empty")
          worker ! NoWorkToBeDone
        }
        else if (workers(worker) == None) {
          log.debug(s"Worker is available [backlogSize=${workQueues.backlogSize}, processSize=${workQueues.processSize}, canProcess=${workQueues.canProcess}, status=${workQueues.status}]")
          if(workQueues.canProcess) {
            val (group, workSender, work) = workQueues.dequeue
            workers += (worker -> Some(workSender -> work))
            // Use the special form of 'tell' that lets us supply the sender
            worker.tell(WorkToBeDone(work), workSender)
          }
        }
      }
    }
    // Worker has completed its work and we can clear it out
    case WorkIsDone(worker) => {
      log.debug(s"Worker is done: $worker")
      if (!workers.contains(worker)) {
        log.error(s"Error: received completed work event, but worker unknown [$worker]")
      } else {
        val work = workers(worker).get
        log.debug(s"Releasing item from processing queue and releasing worker [$work]")
        workQueues.release(lookupGroup(work._2), work._1, work._2)
        // Release the worker
        workers += (worker -> None)
      }
    }
    // A worker died. If it was doing something then we need to give the work 
    // to another worker, so add it back to the master and let things progress 
    // as usual
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
      //TODO initialize limit per domain
      workQueues.enqueue(lookupGroup(work), sender, work)
      notifyWorkers()
    }
  }

}