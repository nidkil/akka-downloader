package com.nidkil.downloader.actor

import java.net.URL
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.matchers.MustMatchers
import Controller.StartDownload
import Splitter.Split
import akka.actor.ActorSystem
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.ImplicitSender

class ControllerTest extends TestKit(ActorSystem("ControllerTest"))
  with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  import Controller._
  import Splitter._

  val actorRef = TestActorRef[Controller]

  override def afterAll() {
    system.shutdown
  }

  "Controller" should {
    "receive messages to start a new download" in {
//      actorRef ! StartDownload(new URL("http://www.nu.nl"))
//      expectMsg(Split)
    }
  }

}