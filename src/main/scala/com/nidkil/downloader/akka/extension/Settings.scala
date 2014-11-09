package com.nidkil.downloader.akka.extension

import com.typesafe.config.Config
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import com.nidkil.downloader.merger.Merger
import com.nidkil.downloader.splitter.Splitter
import com.nidkil.downloader.validator.Validator
import com.nidkil.downloader.cleaner.Cleaner
import com.nidkil.downloader.splitter.DefaultSplitter

class SettingsImpl(config: Config) extends Extension {

  lazy val directory: String = config.getString("downloader.download.directory")
  lazy val forceDownload: Boolean = config.getBoolean("downloader.download.forceDownload")
  lazy val resumeDownload: Boolean = config.getBoolean("downloader.download.resumeDownload")

  def splitter = createInstance[Splitter](config.getString("downloader.dependencies.splitter"))

  def merger = createInstance[Merger](config.getString("downloader.dependencies.merger"))

  def cleaner = createInstance[Cleaner](config.getString("downloader.dependencies.cleaner"))

  def strategy = {
    import scala.reflect.runtime.{ universe => ru }
    val strategyName = config.getString("downloader.dependencies.splitterStrategy")
    val instanceMirror = ru.runtimeMirror(getClass.getClassLoader).reflect(DefaultSplitter)
    val strategyMethod = ru.typeOf[DefaultSplitter.type].declaration(ru.newTermName(strategyName)).asMethod

    instanceMirror.reflectMethod(strategyMethod)
  }

  private def createInstance[T](classFQN: String) = {
    val classLoader = getClass.getClassLoader
    classLoader.loadClass(classFQN).newInstance.asInstanceOf[T]
  }

}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) = {
    new SettingsImpl(system.settings.config)
  }

}