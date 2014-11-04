package com.nidkil.downloader.actor

import akka.actor.ActorLogging
import akka.actor.ActorRef

class ShutdownReaper(controller: ActorRef = null) extends Reaper with ActorLogging {
  
  def allSoulsReaped() {
    log.info(s"All souls reaped, shutting system down")
    
    if(controller != null) context.stop(controller) 
      
    context.system.shutdown()
  }
  
}