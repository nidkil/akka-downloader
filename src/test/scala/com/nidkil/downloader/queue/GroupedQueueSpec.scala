package com.nidkil.downloader.queue

import org.scalatest.WordSpec

class GroupedQueueSpec extends WordSpec {

  def fixtureEmpty =
    new {
      val gq = new GroupedQueue()
    }

  def fixtureOneGroup =
    new {
      val gq = new GroupedQueue()
      gq.createQueue("group-1", 2)
      gq.enqueue("group-1", null, "val-1")
      gq.enqueue("group-1", null, "val-2")
      gq.enqueue("group-1", null, "val-3")
      gq.enqueue("group-1", null, "val-4")
      gq.enqueue("group-1", null, "val-5")
    }

  def fixtureTwoGroupsFiveItems =
    new {
      val gq = new GroupedQueue()
      gq.createQueue("group-1", 2)
      gq.createQueue("group-2", 10)
      gq.enqueue("group-1", null, "val-1")
      gq.enqueue("group-2", null, "val-1")
      gq.enqueue("group-1", null, "val-2")
      gq.enqueue("group-2", null, "val-2")
      gq.enqueue("group-2", null, "val-3")
    }

  def fixtureTwoGroupsTenItems =
    new {
      val gq = new GroupedQueue()
      gq.createQueue("group-1", 2)
      gq.enqueue("group-1", null, "val-1-1")
      gq.enqueue("group-1", null, "val-1-2")
      gq.enqueue("group-1", null, "val-1-3")
      gq.enqueue("group-2", null, "val-2-1")
      gq.enqueue("group-2", null, "val-2-2")
      gq.enqueue("group-2", null, "val-2-3")
      gq.enqueue("group-2", null, "val-2-4")
      gq.enqueue("group-2", null, "val-2-5")
      gq.enqueue("group-2", null, "val-2-6")
      gq.enqueue("group-2", null, "val-2-7")
    }

  "A GroupedQueue" when {
    "when created without valve size" should {
      "have default valve size 4" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        assert(gq.defaultLimit == 4)
      }
    }
    "when no queues have been created" should {
      "add an item if the queue does not exist" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        info("initial checks")
        gq.enqueue("test", null, "val-1")
        assert(!gq.isEmpty)
        assert(gq.nonEmpty)
        assert(!gq.processFull)
        assert(gq.backlogSize == 1)
        assert(gq.processSize == 0)
        assert(gq.limitSize("test") == 4)
        info("dequeue item")
        var (group, actorRef, work) = gq.dequeue
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 1)
        info("produce NoSuchElementException when done is called with wrong values")
        intercept[NoSuchElementException] {
          gq.release(group, actorRef, "no-existing")
        }
        info("item done")
        gq.release(group, actorRef, work)
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 0)
      }
      "not produce an exception when isEmpty is called" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        assert(gq.isEmpty)
      }
      "not produce an exception when nonEmpty is called" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        assert(!gq.nonEmpty)
      }
      "produce true when processFull is called" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        assert(gq.processFull)
      }
      "produce NoSuchElementException when dequeue is called" taggedAs (UnitTest) in {
        intercept[NoGroupsException] {
          val gq = fixtureEmpty.gq
          gq.dequeue
        }
      }
      "produce NoSuchElementException when done is called" taggedAs (UnitTest) in {
        intercept[NoGroupsException] {
          val gq = fixtureEmpty.gq
          gq.release("non-existing", null, null)
        }
      }
      "not produce an exception when clear is called" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        gq.clear
      }
      "no produce NoSuchElementException when backlogSize is called" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        assert(gq.backlogSize == 0)
      }
      "produce NoSuchElementException when processSize is called" taggedAs (UnitTest) in {
        val gq = fixtureEmpty.gq
        assert(gq.processSize == 0)
      }
      "produce NoSuchElementException when limitSize is called" taggedAs (UnitTest) in {
        intercept[NoSuchElementException] {
          val gq = fixtureEmpty.gq
          gq.limitSize("Non existing")
        }
      }
    }
    "when two queues have been created" should {
      "provide information about both queues" taggedAs (UnitTest) in {
        val gq = fixtureTwoGroupsFiveItems.gq

        info("initial checks")
        assert(!gq.isEmpty)
        assert(gq.nonEmpty)
        assert(!gq.processFull)
        assert(gq.backlogSize == 5)
        assert(gq.processSize == 0)
        assert(gq.limitSize("group-1") == 2)
        assert(gq.limitSize("group-2") == 10)

        info("dequeue 3 and checks")
        assert(gq.dequeue != None)
        assert(gq.dequeue != None)
        assert(gq.dequeue != None)
        assert(gq.backlogSize == 2)
        assert(gq.processSize == 3)
        assert(!gq.processFull)

        info("dequeue 1 and checks")
        assert(gq.dequeue != None)
        assert(gq.backlogSize == 1)
        assert(gq.processSize == 4)
        assert(!gq.processFull)

        info("enqueue 1 and checks")
        gq.enqueue("group-1", null, "val-3")
        assert(gq.backlogSize == 2)
        assert(gq.processSize == 4)

        info("dequeue and checks")
        assert(gq.dequeue != None)
        assert(gq.backlogSize == 1)
        assert(gq.processSize == 5)
        assert(!gq.processFull)

        info(s"dequeue throws exception [${gq.status}]")
        intercept[CannotProcessException] {
          gq.dequeue
        }

        info("release second item group-1 and process item")
        gq.release("group-1", null, "val-2")
        assert(gq.dequeue != None)
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 5)

        info("release first item group-1")
        gq.release("group-1", null, "val-1")
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 4)

        info("release group-2 and checks")
        gq.release("group-2", null, "val-3")
        gq.release("group-2", null, "val-2")
        gq.release("group-2", null, "val-1")
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 1)
        assert(!gq.isEmpty)
        assert(gq.nonEmpty)
        assert(!gq.processFull)

        info(s"release on empty processQueue throws exception ${gq.status}")
        gq.release("group-1", null, "val-3")
        intercept[ProcessIsEmptyException] {
          gq.release("group-2", null, "val-1")
        }

        info("clear queue")
        gq.enqueue("group-2", null, "val-5")
        gq.clear
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 0)
        assert(gq.isEmpty)
        assert(!gq.nonEmpty)
        assert(gq.backlogEmpty)
        assert(gq.processEmpty)
        assert(!gq.processFull)
      }
    }
    /*"when two queues have been created" should {
      "provide information about both queues with out specifying a queue" taggedAs (UnitTest) in {
        val gq = fixtureTwoGroupsFiveItems.gq

        info("no group: initial checks")
        assert(!gq.isEmpty)
        assert(gq.nonEmpty)
        assert(gq.backlogSize("group-1") == 2)
        assert(gq.processSize("group-1") == 0)
        assert(gq.backlogSize("group-2") == 3)
        assert(gq.processSize("group-2") == 0)
        assert(gq.backlogSize == 5)
        assert(gq.processSize == 0)

        info("no group: dequeue and checks")
        assert(gq.dequeue("group-1") != None)
        assert(gq.dequeue("group-2") != None)
        assert(!gq.isEmpty)
        assert(gq.nonEmpty)
        assert(gq.backlogSize("group-1") == 1)
        assert(gq.processSize("group-1") == 1)
        assert(gq.backlogSize("group-2") == 2)
        assert(gq.processSize("group-2") == 1)
        assert(gq.backlogSize == 3)
        assert(gq.processSize == 2)

        info("no group: dequeue and checks")
        assert(gq.dequeue("group-1") != None)
        assert(gq.dequeue("group-2") != None)
        assert(gq.dequeue("group-2") != None)
        assert(!gq.isEmpty)
        assert(gq.nonEmpty)
        assert(gq.backlogSize("group-1") == 0)
        assert(gq.processSize("group-1") == 2)
        assert(gq.backlogSize("group-2") == 0)
        assert(gq.processSize("group-2") == 3)
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 5)

        info("no group: dequeue and checks")
        gq.clear
        assert(gq.isEmpty)
        assert(!gq.nonEmpty)
        assert(gq.backlogSize("group-1") == 0)
        assert(gq.processSize("group-1") == 0)
        assert(gq.backlogSize("group-2") == 0)
        assert(gq.processSize("group-2") == 0)
        assert(gq.backlogSize == 0)
        assert(gq.processSize == 0)
      }
    }
    "when two queues have been created" should {
      "be able to manipulate queues without specifying a queue" taggedAs (UnitTest) in {
        val gq = fixtureTwoGroupsTenItems.gq

        info(s"dequeued=${gq.dequeue}")
        assert(gq.backlogSize("group-1") == 2)
        assert(gq.processSize("group-1") == 1)
        assert(gq.backlogSize("group-2") == 3)
        assert(gq.processSize("group-2") == 0)

        info(s"dequeued=${gq.dequeue}")
        assert(gq.backlogSize("group-1") == 2)
        assert(gq.processSize("group-1") == 1)
        assert(gq.backlogSize("group-2") == 2)
        assert(gq.processSize("group-2") == 1)

        info(s"dequeued=${gq.dequeue}")
        assert(gq.backlogSize("group-1") == 1)
        assert(gq.processSize("group-1") == 2)
        assert(gq.backlogSize("group-2") == 2)
        assert(gq.processSize("group-2") == 1)

        info(s"dequeued=${gq.dequeue}")
        assert(gq.backlogSize("group-1") == 1)
        assert(gq.processSize("group-1") == 2)
        assert(gq.backlogSize("group-2") == 1)
        assert(gq.processSize("group-2") == 2)

        info(s"dequeued=${gq.dequeue}")
        assert(gq.backlogSize("group-1") == 1)
        assert(gq.processSize("group-1") == 2)
        assert(gq.backlogSize("group-2") == 0)
        assert(gq.processSize("group-2") == 3)

        gq.release("group-1", null, "val-1-1")
        info(s"dequeued=${gq.dequeue} [${gq.status}]")
        assert(gq.backlogSize("group-1") == 0)
        assert(gq.processSize("group-1") == 2)
        assert(gq.backlogSize("group-2") == 0)
        assert(gq.processSize("group-2") == 3)
      }
    }*/
  }

}