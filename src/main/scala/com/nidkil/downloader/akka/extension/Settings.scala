package com.nidkil.downloader.akka.extension

import com.typesafe.config.Config

import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider

class SettingsImpl(config: Config) extends Extension {

  val directory: String = config.getString("downloader.download.directory")
  val forceDownload: Boolean = config.getBoolean("downloader.download.forceDownload")
  val resumeDownload: Boolean = config.getBoolean("downloader.download.resumeDownload")
  
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
  
  override def lookup = Settings
  
  override def createExtension(system: ExtendedActorSystem) = {
    new SettingsImpl(system.settings.config)
  }

}