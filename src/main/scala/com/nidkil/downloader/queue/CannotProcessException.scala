package com.nidkil.downloader.queue

class CannotProcessException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  
  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)
  
}