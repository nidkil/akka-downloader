package com.nidkil.downloader.queue

import org.scalatest.Matchers
import org.scalatest.Tag
import org.scalatest.WordSpec
import akka.actor.ActorRef

object UnitTest extends Tag("com.nidkil.tags.UnitTest")

class BacklogQueueSpec extends WordSpec {

  def fixtureEmpty =
    new {
      val bq = new BacklogQueue()
    }

  def fixtureFive =
    new {
      val bq = new BacklogQueue(2)
      bq.enqueue(null, "test-1")
      bq.enqueue(null, "test-2")
      bq.enqueue(null, "test-3")
      bq.enqueue(null, "test-4")
      bq.enqueue(null, "test-5")
    }

  "A BacklogQueue" when {
    "created without a specific limit" should {
      val bq = fixtureEmpty.bq
      "have a default limit of 2" taggedAs (UnitTest) in {
        assert(bq.limit == 2)
      }
    }
    "empty" should {
      "have size 0" taggedAs (UnitTest) in {
        val bq = fixtureEmpty.bq
        assert(bq.backlogSize == 0)
        assert(bq.processSize == 0)
      }
      "be empty if both backlog and process are empty" taggedAs (UnitTest) in {
        val bq = fixtureEmpty.bq
        assert(bq.isEmpty)
      }
      "not be empty if both backlog and process are empty" taggedAs (UnitTest) in {
        val bq = fixtureEmpty.bq
        assert(!bq.nonEmpty)
      }
      "produce BacklogIsEmptyException when dequeue is invoked" taggedAs (UnitTest) in {
        intercept[BacklogIsEmptyException] {
          val bq = fixtureEmpty.bq
          bq.dequeue
        }
      }
      "produce NoSuchElementException when release is invoked" taggedAs (UnitTest) in {
        intercept[ProcessIsEmptyException] {
          val bq = fixtureEmpty.bq
          bq.release(null, "test")
        }
      }
    }
    "5 items are added" should {
      "have size 5" in {
        val bq = fixtureFive.bq
        assert(bq.limit == 2)
        assert(bq.backlogSize == 5)
        assert(bq.processSize == 0)
      }
      "not be empty" in {
        val bq = fixtureFive.bq
        assert(!bq.isEmpty)
        assert(!bq.backlogEmpty)
        assert(bq.processEmpty)
      }
      "be non empty" in {
        val bq = fixtureFive.bq
        assert(bq.nonEmpty)
        assert(bq.backlogNonEmpty)
        assert(!bq.processNonEmpty)
      }
    }
    "1 item is dequeued" should {
      "pending should have size 4 and working size 1" in {
        val bq = fixtureFive.bq
        val work = bq.dequeue
        assert(bq.backlogSize == 4)
        assert(bq.processSize == 1)
        assert(!bq.isEmpty)
        assert(bq.nonEmpty)
        assert(!bq.processEmpty)
        assert(bq.processNonEmpty)
        assert(!bq.processFull)
      }
    }
    "another item is dequeued" should {
      "pending should have size 3 and working size 2" in {
        val bq = fixtureFive.bq
        val work1 = bq.dequeue
        val work2 = bq.dequeue
        assert(bq.backlogSize == 3)
        assert(bq.processSize == 2)
        assert(!bq.isEmpty)
        assert(bq.nonEmpty)
        assert(!bq.processEmpty)
        assert(bq.processNonEmpty)
        assert(bq.processFull)
      }
    }
    "another item is dequeued and the valve is maxed" should {
      "pending should still be size 3 and working still size 2, work should throw an exception" in {
        val bq = fixtureFive.bq
        val work1 = bq.dequeue
        assert(bq.processNonEmpty)
        val work2 = bq.dequeue
        assert(!bq.processEmpty)
        assert(bq.processNonEmpty)
        assert(bq.processFull)
        intercept[ProcessIsFullException] {
          val workEx = bq.dequeue
        }
        assert(bq.backlogSize == 3)
        assert(bq.processSize == 2)
      }
    }
    "an item is done" should {
      "pending should still be size 3 and working should be size 1" in {
        val bq = fixtureFive.bq
        val work1 = bq.dequeue
        val work2 = bq.dequeue
        bq.release(null, "test-1")
        assert(bq.backlogSize == 3)
        assert(bq.processSize == 1)
      }
    }
    "all items are dequeued" should {
      "dequeue should return None" in {
        val bq = fixtureFive.bq
        var work1 = bq.dequeue
        var work2 = bq.dequeue
        assert(work1 == (null, "test-1"))
        assert(work2 == (null, "test-2"))
        bq.release(work1._1, work1._2)
        bq.release(work2._1, work2._2)
        work1 = bq.dequeue
        work2 = bq.dequeue
        assert(work1 == (null, "test-3"))
        assert(work2 == (null, "test-4"))
        bq.release(work1._1, work1._2)
        bq.release(work2._1, work2._2)
        work1 = bq.dequeue
        assert(work1 == (null, "test-5"))
        assert(!bq.isEmpty)
        assert(bq.nonEmpty)
        intercept[Exception] {
          work2 = bq.dequeue
        }
        bq.release(work1._1, work1._2)
        assert(bq.isEmpty)
        assert(!bq.nonEmpty)
        intercept[ProcessIsEmptyException] {
          bq.release(null, work1)
        }
        assert(bq.backlogSize == 0)
        assert(bq.processSize == 0)
      }
    }
    "clearing the queue" should {
      val bq = fixtureFive.bq
      bq.clear
      "be empty" in {
        assert(bq.isEmpty)
      }
      "both pending and working be size 0" in {
        assert(bq.backlogSize == 0)
        assert(bq.processSize == 0)
      }
    }
  }

}