package com.nidkil.downloader.cli

import java.net.URL
import com.nidkil.downloader.actor.Controller
import com.nidkil.downloader.actor.Controller.NewDownload
import com.nidkil.downloader.actor.Monitor
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.RoundRobinPool
import com.nidkil.downloader.actor.ShutdownReaper
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.routing.Broadcast

object DownloaderApp extends App {

  import Controller._

  override def main(args: Array[String]) {
    val system = ActorSystem("System", ConfigFactory.load("application.conf"))
    //TODO make instances configurable
    //TODO move router to separate actor
    val controllerRouter = system.actorOf(RoundRobinPool(1).props(Props[Controller]), "controller")
    //TODO we are passing a router, does stop shutdown the router and the actors it controles?
    val shutdownReaper = system.actorOf(Props(new ShutdownReaper(controllerRouter)), "shutdownReaper")

    controllerRouter ! Broadcast(Startup(shutdownReaper))

    // Give controllers time to startup
    Thread.sleep(2000)

    // The checksums of these files can be found at the bottom of the webpage (search for MD5SUMS)
    //TODO add option to set final name of downloaded file, so that it is possible to download the same file multiple times
    controllerRouter ! NewDownload(new URL("http://download.thinkbroadband.com/5MB.zip"), "b3215c06647bc550406a9c8ccc378756")
//    controllerRouter ! NewDownload(new URL("http://download.thinkbroadband.com/2MB.zip"), "528972766cd55c26a570829775afd2a8")
//    controllerRouter ! NewDownload(new URL("http://download.thinkbroadband.com/20MB.zip"), "9017804333c820e3b4249130fc989e00")
//    controllerRouter ! NewDownload(new URL("http://download.thinkbroadband.com/10MB.zip"), "3aa55f03c298b83cd7708e90d289afbd")
//    controllerRouter ! NewDownload(new URL("http://download.thinkbroadband.com/50MB.zip"), "2699c63cb6699b2272f78989b09e88b1")
//    controllerRouter ! NewDownload(new URL("http://download.thinkbroadband.com/100MB.zip"), "5b563100babfef2f2ec9ab2d55e97fd1")
  }

}