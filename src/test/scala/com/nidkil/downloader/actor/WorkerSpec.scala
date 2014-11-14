package com.nidkil.downloader.actor

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import com.typesafe.config.ConfigFactory
import akka.actor.ActorPath
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.util.Timeout
import com.nidkil.downloader.protocol.MasterWorkerProtocol

class WorkerSpec extends TestKit(ActorSystem("WorkerSpec", ConfigFactory.parseString(WorkerSpec.config)))
    with DefaultTimeout with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  import WorkerSpec._
  import org.scalatest.Matchers._
  import scala.concurrent.duration._
  import MasterWorkerProtocol._

  implicit val askTimeout = Timeout(1 second)
   
  override def afterAll {
    shutdown()
  }
  
  def worker(name: String) = system.actorOf(Props(
    new TestWorker(ActorPath.fromString(s"akka://${system.name}/user/$name"))))

  def badWorker(name: String) = system.actorOf(Props(
    new BadTestWorker(ActorPath.fromString(s"akka://${system.name}/user/$name"))))

  "Worker" should {
    "work" in {
      val master = system.actorOf(Props[Master], "master-1")
      val worker1 = worker("master-1")
      val worker2 = worker("master-1")
      val worker3 = worker("master-1")

      master ! "Hi there"
      master ! "Guys"
      master ! "So"
      master ! "What's"
      master ! "Up?"

      expectMsgAllOf("Hi there", "Guys", "So", "What's", "Up?")
    }
    "still work if one dies" in {
      val master = system.actorOf(Props[Master], "master-2")
      val worker1 = worker("master-2")
      val worker2 = badWorker("master-2")
      
      master ! "Hi there"
      master ! "Guys"
      master ! "So"
      master ! "What's"
      master ! "Up?"

      expectMsgAllOf("Hi there", "Guys", "So", "What's", "Up?")
    }
    "work with Futures" in {
      val master = system.actorOf(Props[Master], "master-3")
      val worker1 = worker("master-3")
      val worker2 = worker("master-3")
      val worker3 = worker("master-3")
      
      val listOfMessages = List("Hi there", "Guys", "So", "What's", "Up?")
      val listOfFutures = listOfMessages.map(akka.pattern.ask(master, _).mapTo[String])
      implicit val ec = system.dispatcher
      val futureList = Future.sequence(listOfFutures)
      val result = Await.result(futureList, 1 second)
      
      assert(listOfMessages == result)
    }
    /**"work with funneled master" in {
      val funneledMaster = system.actorOf(Props[FunneledMaster], "master-4")
      val worker1 = worker("master-1")
      val worker2 = worker("master-1")
      val worker3 = worker("master-1")

      funneledMaster ! FunnelWork("test-1", "Hi there")
      funneledMaster ! FunnelWork("test-1", "Guys")
      funneledMaster ! FunnelWork("test-1", "So")
      funneledMaster ! FunnelWork("test-1", "What's")
      funneledMaster ! FunnelWork("test-1", "Up?")

      funneledMaster ! FunnelWork("test-2", "This")
      funneledMaster ! FunnelWork("test-2", "is")
      funneledMaster ! FunnelWork("test-2", "my")
      funneledMaster ! FunnelWork("test-2", "second")
      funneledMaster ! FunnelWork("test-2", "batch")
      funneledMaster ! FunnelWork("test-2", "of")
      funneledMaster ! FunnelWork("test-2", "messages")

      expectMsgAllOf("Hi there", "Guys", "So", "What's", "Up?", "This", "is", "my", "second", "batch", "of", "messages")
    }*/
  }

}

object WorkerSpec {
  // Define the test specific configuration
  val config = """
    akka {
      loglevel = "WARNING"
    }"""

  class TestWorker(masterLocation: ActorPath) extends Worker(masterLocation) {
    // Set an execution context
    implicit val ec = context.dispatcher
    def doWork(workSender: ActorRef, msg: Any): Unit = {
      Future {
        workSender ! msg
        WorkComplete("done")
      } pipeTo self
    }
  }

  class BadTestWorker(masterLocation: ActorPath) extends Worker(masterLocation) {
    // Set an execution context
    implicit val ec = context.dispatcher
    def doWork(workSender: ActorRef, msg: Any): Unit = context.stop(self)
  }

}