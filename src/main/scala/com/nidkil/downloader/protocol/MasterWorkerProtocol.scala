package com.nidkil.downloader.protocol

import akka.actor.ActorRef

/**
 * Adapted from https://github.com/derekwyatt/akka-worker-pull
 */
object MasterWorkerProtocol {
  // Messages for FunneledMaster
  case class FunnelWork(domain: String, work: Any)
  case object FunnelRemoveIdle
  
  // Messages from Workers
  case class WorkerCreated(worker: ActorRef)
  case class WorkerRequestsWork(worker: ActorRef)
  case class WorkIsDone(worker: ActorRef)

  // Messages to Workers
  case class WorkToBeDone(work: Any)
  case object WorkIsReady
  case object NoWorkToBeDone
}