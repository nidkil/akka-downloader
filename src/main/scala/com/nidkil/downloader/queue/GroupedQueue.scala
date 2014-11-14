package com.nidkil.downloader.queue

import akka.actor.ActorRef
import com.nidkil.downloader.utils.Logging

//TODO cleanup queues after a periode of being idle
//TODO http://java.dzone.com/articles/applying-back-pressure-when
/**
 * This queue implementation manages BacklogQueue per domain.
 * 
 * 
 * Note: This is not meant to be generic implementation, but specific for
 * the Akka Master Worker pattern.
 */
class GroupedQueue(val defaultLimit: Int = 4) extends Logging {

  import scala.collection.mutable.Map

  private lazy val groupQueues = Map.empty[String, BacklogQueue]

  def createQueue(group: String, limit: Int = defaultLimit): Unit = {
    if (groupQueues.contains(group)) throw new IllegalArgumentException("Group already exists")
    groupQueues += group -> new BacklogQueue(limit)
  }

  def noGroups: Boolean = groupQueues.size == 0
  
  def isEmpty: Boolean = groupQueues.filter(_._2.isEmpty).size == groupQueues.size

  def nonEmpty: Boolean = groupQueues.filter(_._2.nonEmpty).size > 0

  def backlogEmpty: Boolean = groupQueues.filter(_._2.backlogEmpty).size == groupQueues.size

  def processEmpty: Boolean = groupQueues.filter(_._2.processEmpty).size == groupQueues.size

  def processFull: Boolean = groupQueues.filter(_._2.processFull).size == groupQueues.size

  def canProcess: Boolean = groupQueues.filter(x => !x._2.processFull && !x._2.backlogEmpty).size > 0

  def enqueue(group: String, sender: ActorRef, work: Any): Unit = {
    if (!groupQueues.contains(group)) 
      groupQueues += group -> new BacklogQueue(defaultLimit)
    groupQueues(group).enqueue(sender, work)
  }
  
  def dequeue: (String, ActorRef, Any) = {
    if(noGroups)
      throw new NoGroupsException("No groups defined")
    if(processFull)
      throw new ProcessIsFullException("Process queues are full")
    if(backlogEmpty)
      throw new BacklogIsEmptyException("Backlog queues are empty")
    if(!canProcess)
      throw new CannotProcessException("Process queues are full or backlog is empty")
    val group = QueueScheduling.schedule(groupQueues)
    logger.trace(s"key=$group")
    val item = group.map(groupQueues(_).dequeue).get
    (group.get, item._1, item._2)
  }

  def release(group: String, sender: ActorRef, work: Any): Unit = {
    if(noGroups)
      throw new NoGroupsException("No groups defined")
    if(processEmpty)
      throw new ProcessIsEmptyException("Process queues are empty")
    groupQueues(group).release(sender, work)
  }

  def clear: Unit = groupQueues.foreach(_._2.clear)

  def backlogSize: Int = groupQueues.map(_._2.backlogSize).sum

  def processSize: Int = groupQueues.map(_._2.processSize).sum

  def limitSize(group: String): Int = groupQueues(group).limit
  
  def status: String = groupQueues.map(_._2.status).mkString(", ")

}