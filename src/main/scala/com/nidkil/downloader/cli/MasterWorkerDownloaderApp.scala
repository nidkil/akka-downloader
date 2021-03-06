package com.nidkil.downloader.cli

import java.net.URL

import com.nidkil.downloader.actor.Controller
import com.nidkil.downloader.actor.Controller.DownloadNew
import com.nidkil.downloader.actor.Controller.Startup
import com.nidkil.downloader.actor.DownloadWorker
import com.nidkil.downloader.actor.DownloadWorker.ChunkDownload
import com.nidkil.downloader.actor.FunneledMaster
import com.nidkil.downloader.actor.Monitor
import com.nidkil.downloader.actor.ShutdownReaper
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

import akka.actor.ActorPath
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.Broadcast
import akka.routing.FromConfig

object MasterWorkerDownloaderApp extends App {

  import Controller._
  import DownloadWorker._

  def prettyPrintConfig(system: ActorSystem): Unit = {
    val config = ConfigFactory.load("application.conf")
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)
    println(system.settings.config.root().render(renderOpts))
  }

  def getDownloads = Set(
    DownloadNew(new URL("http://mirror.nl.webzilla.com/apache/tomcat/tomcat-7/v7.0.57/bin/apache-tomcat-7.0.57.zip"), "2bc8949a9c2ac44c5787b9ed4cfd3d0d"),
    DownloadNew(new URL("http://download.thinkbroadband.com/5MB.zip"), "b3215c06647bc550406a9c8ccc378756"),
    DownloadNew(new URL("http://download.thinkbroadband.com/2MB.zip"), "528972766cd55c26a570829775afd2a8"),
    DownloadNew(new URL("http://download.thinkbroadband.com/20MB.zip"), "9017804333c820e3b4249130fc989e00"),
    DownloadNew(new URL("http://download.thinkbroadband.com/10MB.zip"), "3aa55f03c298b83cd7708e90d289afbd"),
    DownloadNew(new URL("http://download.thinkbroadband.com/50MB.zip"), "2699c63cb6699b2272f78989b09e88b1"),
    DownloadNew(new URL("http://download.thinkbroadband.com/100MB.zip"), "5b563100babfef2f2ec9ab2d55e97fd1"),
    DownloadNew(new URL("http://mirror.nl.webzilla.com/apache/tomcat/tomcat-7/v7.0.57/bin/apache-tomcat-7.0.57.zip"), "2bc8949a9c2ac44c5787b9ed4cfd3d0d"),
    //TODO check URL does not exist
    DownloadNew(new URL("http://mirrors.supportex.net/apache/tomcat/tomcat-7/v7.0.56/bin/apache-tomcat-7.0.56.tar.gz"), "2887d0e3ca18bdca63004a0388c99775"))

  def lookupStrategy(work: Any): String = work.asInstanceOf[ChunkDownload].chunk.url.getHost

  def worker(system: ActorSystem, name: String, monitor: ActorRef) = system.actorOf(Props(
    new DownloadWorker(ActorPath.fromString(s"akka://${system.name}/user/$name"), monitor)))

  val system = ActorSystem("Downloader", ConfigFactory.load("application.conf"))
  val monitor = system.actorOf(Props[Monitor], "monitor")
  val master = system.actorOf(Props(new FunneledMaster(lookupStrategy)), "master")
  //TODO make number of workers configurable
  for (i <- 1 to 6) worker(system, "master", monitor)

  val controllerRouter = system.actorOf(FromConfig.props(Props(new Controller(master, monitor))), "controllerRouter")

  //TODO we are passing a router, does stop shutdown the router and the actors it controles?
  val shutdownReaper = system.actorOf(Props(new ShutdownReaper(controllerRouter)), "shutdownReaper")

  //TODO remove the router? As we now create separate handling context for downloads?
  controllerRouter ! Broadcast(Startup(shutdownReaper))

  // The checksums of these files can be found at the bottom of the webpage (search for MD5SUMS)
  //TODO add option to set final name of downloaded file, so that it is possible to download the same file multiple times
  getDownloads.takeRight(2).foreach(controllerRouter ! _)

}