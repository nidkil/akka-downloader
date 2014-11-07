package com.nidkil.downloader.cli

import java.net.URL

import com.nidkil.downloader.actor.Controller
import com.nidkil.downloader.actor.Controller.DownloadNew
import com.nidkil.downloader.actor.Controller.Startup
import com.nidkil.downloader.actor.ShutdownReaper
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.Broadcast
import akka.routing.FromConfig

object DownloaderApp extends App {

  import Controller._

  def prettyPrintConfig(system: ActorSystem): Unit = {
    val config = ConfigFactory.load("application.conf")
    val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)
    println(system.settings.config.root().render(renderOpts))
  }

  override def main(args: Array[String]) {
    val system = ActorSystem("Downloader", ConfigFactory.load("application.conf"))
    val controllerRouter = system.actorOf(FromConfig.props(Props[Controller]), "controllerRouter")
    //TODO we are passing a router, does stop shutdown the router and the actors it controles?
    val shutdownReaper = system.actorOf(Props(new ShutdownReaper(controllerRouter)), "shutdownReaper")

    //TODO remove the router? As we now create separate handling context for downloads?
    controllerRouter ! Broadcast(Startup(shutdownReaper))

    // The checksums of these files can be found at the bottom of the webpage (search for MD5SUMS)
    //TODO add option to set final name of downloaded file, so that it is possible to download the same file multiple times
    //    controllerRouter ! DownloadNew(new URL("http://download.thinkbroadband.com/5MB.zip"), "b3215c06647bc550406a9c8ccc378756")
//    controllerRouter ! DownloadNew(new URL("http://download.thinkbroadband.com/2MB.zip"), "528972766cd55c26a570829775afd2a8")
    controllerRouter ! DownloadNew(new URL("http://download.thinkbroadband.com/20MB.zip"), "9017804333c820e3b4249130fc989e00")
    controllerRouter ! DownloadNew(new URL("http://download.thinkbroadband.com/10MB.zip"), "3aa55f03c298b83cd7708e90d289afbd")
    controllerRouter ! DownloadNew(new URL("http://download.thinkbroadband.com/50MB.zip"), "2699c63cb6699b2272f78989b09e88b1")
//    controllerRouter ! DownloadNew(new URL("http://download.thinkbroadband.com/100MB.zip"), "5b563100babfef2f2ec9ab2d55e97fd1")
    controllerRouter ! DownloadNew(new URL("http://mirrors.supportex.net/apache/tomcat/tomcat-7/v7.0.56/bin/apache-tomcat-7.0.56.zip"), "2bc8949a9c2ac44c5787b9ed4cfd3d0d")
    controllerRouter ! DownloadNew(new URL("http://mirrors.supportex.net/apache/tomcat/tomcat-7/v7.0.56/bin/apache-tomcat-7.0.56.tar.gz"), "2887d0e3ca18bdca63004a0388c99775")
  }

}