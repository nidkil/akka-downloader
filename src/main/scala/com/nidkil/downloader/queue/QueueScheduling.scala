package com.nidkil.downloader.queue

import scala.compat.Platform._
import scala.math.pow

case class Variable(
  pendingQueueSize: Int = 0,
  workingQueueSize: Int = 0,
  numQueues: Int = 1,
  var avg: Double = 0,
  var startIdleTime: Long = currentTime,
  var count: Int = -1)
case class Fixed(
  queueWeight: Double = 0.0002,
  minThreshold: Int = 1,
  maxThreshold: Double = 4,
  maxp: Double = 0.02)

object QueueScheduling {

  import scala.collection.mutable.Map

  private def queueEmpty(vars: Variable): Variable = vars.copy(startIdleTime = currentTime)
  
  private def calcAvgQueueSize(vars: Variable, fixed: Fixed): Variable = {
    val newVars = vars.copy()
    if (vars.pendingQueueSize == 0) {
      val m = currentTime - vars.startIdleTime
      //TODO missing m      
      newVars.avg = pow((1 - fixed.queueWeight), m) * vars.numQueues
    } else {
      newVars.avg = (1 - fixed.queueWeight) * vars.avg + fixed.queueWeight * vars.pendingQueueSize
    }
    println(s" -- newVars.avg=${newVars.avg}")
    if (fixed.minThreshold <= newVars.avg && newVars.avg < fixed.maxThreshold) {
      newVars.count += 1
      val pb = fixed.maxp * (newVars.avg - fixed.minThreshold) / (fixed.maxThreshold - fixed.minThreshold)
      val pa = pb / (1 - newVars.count * pb)
    } else if (fixed.maxThreshold <= newVars.avg) {
      newVars.count = 0
    } else {
      newVars.count = -1
    }
    newVars
  }

  def schedule(queues: Map[String, BacklogQueue]): Option[String] = {
    val queuesFiltered = queues.filter(x => !x._2.processFull && !x._2.backlogEmpty)
    // Lets be efficient, if only no queues available stop processing
    if (queuesFiltered.size == 0) None
    // Lets be efficient, if only one queue available grab it
    if (queuesFiltered.size == 1) Some(queuesFiltered.head._1)
    else {
      // First schedule queues with working queues that are empty, so that 
      // every queue in theory should hava at least one item processing
      val valveEmpty = queuesFiltered.filter(_._2.processEmpty)
      if (valveEmpty.size > 0) Some(valveEmpty.head._1)
      else {
        // This is were things get interesting
        //TODO scheduling algorithm
        Some(queuesFiltered.head._1)
      }
    }
  }

}