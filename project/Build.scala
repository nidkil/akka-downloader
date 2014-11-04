import sbt._

object Version {
  val scalaDownloader = "v0.5.4"
}

object Projects {
	lazy val scalaDownloader = RootProject(uri(s"https://github.com/nidkil/ScalaDownloader.git#${Version.scalaDownloader}"))
}

object MyBuild extends Build {
	lazy val root = Project("root", file(".")).dependsOn(Projects.scalaDownloader)
}