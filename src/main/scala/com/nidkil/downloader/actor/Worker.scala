package com.nidkil.downloader.actor

import akka.actor.{ Actor, ActorLogging, ActorPath, ActorRef }
import com.nidkil.downloader.protocol.MasterWorkerProtocol

abstract class Worker(masterPath: ActorPath) extends Actor with ActorLogging {
  import MasterWorkerProtocol._

  // We need to know where the master is
  val master = context.actorSelection(masterPath)

  // This is how worker derivation interact with itself. It allows dervations 
  // to complete work asynchronously
  case class WorkComplete(result: Any)

  // Handles the actual processing of work, required: must be implemented
  def doWork(workSender: ActorRef, work: Any): Unit

  // Notify the Master that worker is available
  override def preStart() = master ! WorkerCreated(self)

  // In this state worker is working on something. Swapping states enables 
  // actor to deal with messages in a more elegant way and control what
  // the actor works on
  def working(work: Any): Receive = {
    // Ignore, should not receive this event
    case WorkIsReady =>
      log.warning(s"Received WorkIsReady event, already working")
    // Ignore, should not receive this event
    case NoWorkToBeDone =>
      log.warning(s"Received NoWorkToBeDone event, already working")
    // Ignore, should not receive this event
    case WorkToBeDone(_) =>
      log.error("Received work from Master, already working")
    // Worker has completed its task
    case WorkComplete(result) =>
      log.info(s"Work complete, result: $result")
      master ! WorkIsDone(self)
      master ! WorkerRequestsWork(self)
      // Change to idle state
      context.become(idle)
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }

  // In this state worker is idle. Swapping states enables actor to deal with 
  // messages in a more elegant way and control what the actor works on
  def idle: Receive = {
    // Master says there is work available, let's ask for it
    case WorkIsReady =>
      //      log.info("Requesting work")
      master ! WorkerRequestsWork(self)
    // Received work, lets process it
    case WorkToBeDone(work) =>
      log.info(s"Received work: $work")
      doWork(sender, work)
      context.become(working(work))
    // We asked for it, but either someone else got it first, or
    // there's literally no work to be done
    case NoWorkToBeDone =>
    case x => log.warning(s"Unknown message received by ${self.path} from ${sender.path} [${x.getClass}, value=$x]")
  }

  def receive = idle

}