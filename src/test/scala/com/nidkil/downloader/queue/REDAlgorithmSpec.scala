package com.nidkil.downloader.queue

import org.scalatest.WordSpec

// Random Early Detection algorithm
class REDAlgorithmSpec extends WordSpec {

  "A REDQueue" when {
    "when created" should {
      "have size 2" taggedAs (UnitTest) in {
        val queueVars1 = REDVariable()
        val queueVars2 = REDVariable(curQueueSize = 10)
        val queueFixed1 = REDFixed(minth = 1, maxth = 2)
        val queueFixed2 = REDFixed(minth = 1, maxth = 4)
        val queue1Result1 = REDAlgorithm.calcAvgQueueSize(queueVars1, queueFixed1)
        val queue2Result1 = REDAlgorithm.calcAvgQueueSize(queueVars2, queueFixed2)
        println(s"queue1Result1=$queue1Result1")
        println(s"queue2Result1=$queue2Result1")
        //val rc = queue1Result1.copy(curQueueSize = 5)
//        val queue1Result2 = REDAlgorithm.calcAvgQueueSize(rc, fixed)
//        println(queue1Result2)
        //assert(bq.valveSize == 2)
      }
    }
  }

}