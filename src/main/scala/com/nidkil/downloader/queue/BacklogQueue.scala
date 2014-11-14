package com.nidkil.downloader.queue

import akka.actor.ActorRef

/**
 * This queue implementation limits the throughput to a resource.
 * 
 * The queue acts like a gateway to a resource. It consists of two queues:
 * - The first queue handles the backlog ensuring items are queued until
 *   they can be handled.
 * - The second queue is the processing queue and which tracks items being
 *   processed. The size of this queue is limited to the number of 
 *   concurrent requests that can be handled by the resource.
 * 
 * Enqueue adds an item to the backlog queue. Dequeue moves an item from the 
 * backlog queue to the process queue. When an item has been completed by the 
 * resource it must be released from the process queue by calling release.
 * 
 * Note: This is not meant to be generic implementation, but specific for
 * the Akka Master Worker pattern.
 */
class BacklogQueue(val limit: Int = 2) {

  import scala.collection.mutable.Queue

  private lazy val processQueue = Queue.empty[(ActorRef, Any)]
  private lazy val backlogQueue = Queue.empty[(ActorRef, Any)]

  def isEmpty: Boolean = (processQueue.isEmpty && backlogQueue.isEmpty)

  def nonEmpty: Boolean = (processQueue.nonEmpty || backlogQueue.nonEmpty)

  def backlogEmpty: Boolean = backlogQueue.isEmpty

  def backlogNonEmpty: Boolean = backlogQueue.nonEmpty
  
  def processFull: Boolean = processQueue.size == limit
  
  def processEmpty: Boolean = processQueue.size == 0
  
  def processNonEmpty: Boolean = processQueue.size > 0

  def enqueue(sender: ActorRef, work: Any): Unit = backlogQueue.enqueue((sender, work))

  def dequeue: (ActorRef, Any) = {
    if(processFull)
      throw new ProcessIsFullException("Process queue is full, first call done to free space")
    if(backlogEmpty)
      throw new BacklogIsEmptyException("Backlog queue is empty")
    val work = backlogQueue.dequeue
    processQueue.enqueue(work)
    work
  }

  def release(sender: ActorRef, work: Any): Unit = {
    if(processEmpty)
      throw new ProcessIsEmptyException("Process is empty")
    if(processQueue.dequeueAll(x => (x._1 == sender && x._2 == work)).size == 0)
      throw new NoSuchElementException("Specified item not found in queue")
  }

  def clear: Unit = {
    backlogQueue.clear 
    processQueue.clear 
  }

  def totalSize: Int = backlogQueue.size + processQueue.size
  
  def backlogSize: Int = backlogQueue.size
  
  def processSize: Int = processQueue.size
  
  def status: String = s"[limit=$limit, backlogSize=$backlogSize, processSize=$processSize]"

}