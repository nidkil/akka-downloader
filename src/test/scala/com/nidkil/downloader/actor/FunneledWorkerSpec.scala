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

class FunneledWorkerSpec extends TestKit(ActorSystem("FunneledWorkerSpec", ConfigFactory.parseString(FunneledWorkerSpec.config)))
    with DefaultTimeout 
    with ImplicitSender 
    with WordSpecLike 
    with Matchers 
    with BeforeAndAfterAll {

  import FunneledWorkerSpec._
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
      val master = system.actorOf(Props(new FunneledMaster(FunneledWorkerSpec.lookupStrategy)), "master-1")
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
      val master = system.actorOf(Props(new FunneledMaster(FunneledWorkerSpec.lookupStrategy)), "master-2")
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
      val master = system.actorOf(Props(new FunneledMaster(FunneledWorkerSpec.lookupStrategy)), "master-3")
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
  }

}

object FunneledWorkerSpec {
  // Define the test specific configuration
  val config = """
    akka {
      loglevel = "DEBUG"
    }"""

  def lookupStrategy(work: Any): String = {
    if(work.asInstanceOf[String].startsWith("H")) "test-1"
    else "test-2"
  }

  class TestWorker(masterLocation: ActorPath) extends Worker(masterLocation) {
    // Set an execution context
    implicit val ec = context.dispatcher
    def doWork(workSender: ActorRef, msg: Any): Unit = {
      // Execute the future and pipe the result to self (pipeTo self)
      Future {
        workSender ! msg
        WorkComplete("done")
      } pipeTo self
    }
  }

  class BadTestWorker(masterLocation: ActorPath) extends Worker(masterLocation) {
    // Set an execution context
    implicit val ec = context.dispatcher
    def doWork(workSender: ActorRef, msg: Any): Unit = {
      context.stop(self)
    }
  }

}