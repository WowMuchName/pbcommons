package net.pbforge.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TimeTableTest : StringSpec({
    "should not be able to create invalid table" {
        shouldThrow<IllegalStateException> {
            RingBufferTimeTable(0)
        }
        shouldThrow<IllegalStateException> {
            RingBufferTimeTable(2, 1)
        }
    }

    "should create table with defaults" {
        val timeTable = RingBufferTimeTable()
        timeTable.currentBufferSize shouldBe 64
        timeTable.maxBufferSize shouldBe 4096
    }
    val timeTable = RingBufferTimeTable(4, 8)

    "should add up sums in empty table" {
        timeTable.sumBefore(0) shouldBe 0
        timeTable.sumAfter(0) shouldBe 0
    }

    "should not be able to expand a table past its maxBufferSize" {
        shouldThrow<IllegalStateException> {
            timeTable.resizeBuffer(9)
        }.message shouldBe "newBufferSize (9) is bigger than the maxBufferSize (8)"
    }

    "purging an empty table should work" {
        timeTable.purgeEntriesBefore(Long.MAX_VALUE)
        timeTable.entryCount shouldBe 0
    }

    "should be able to add entries to a table" {
        timeTable.addEntry(1, 0)
        timeTable.addEntry(2, 1)
        timeTable.addEntry(4, 2)
        timeTable.currentBufferSize shouldBe 4
        timeTable.entryCount shouldBe 3
        timeTable.currentSum shouldBe 7
        timeTable.sumBefore(1) shouldBe 1
        timeTable.sumAfter(1) shouldBe 4
    }

    "should add up sum in non empty table" {
        timeTable.sumBefore(1) shouldBe 1
        timeTable.sumAfter(1) shouldBe 4

        timeTable.sumBefore(100) shouldBe 7
        timeTable.sumAfter(-1) shouldBe 7

        timeTable.sumBefore(0) shouldBe 0
        timeTable.sumBefore(-1) shouldBe 0
        timeTable.sumAfter(2) shouldBe 0
        timeTable.sumAfter(100) shouldBe 0
    }

    "should return minimum time a sum is reached" {
        timeTable.minimumSumReached(3) shouldBe 1
        timeTable.minimumSumReached(6) shouldBe 2
        timeTable.minimumSumReached(7) shouldBe 2
        timeTable.minimumSumReached(8) shouldBe null
    }

    "should not be able to create entry old than an existing entry" {
        shouldThrow<IllegalStateException> {
            timeTable.addEntry(1, 1)
        }.message shouldBe "An entry that happened after the specified time already exist (1 time-units before)"
    }

    "should be able to purge from initial read position" {
        timeTable.purgeEntriesBefore(1)
        timeTable.entryCount shouldBe 2
        timeTable.currentSum shouldBe 6
        timeTable.sumBefore(1) shouldBe 0
        timeTable.sumAfter(0) shouldBe 6
    }

    "should be able to purge from a non initial read position" {
        timeTable.purgeEntriesBefore(2)
        timeTable.entryCount shouldBe 1
        timeTable.currentSum shouldBe 4
        timeTable.sumBefore(3) shouldBe 4
        timeTable.sumAfter(1) shouldBe 4
    }

    "should be able to purge from a read position that overflows" {
        timeTable.addEntry(8, 3)
        timeTable.addEntry(16, 4)
        timeTable.entryCount shouldBe 3
        timeTable.currentSum shouldBe 28
        timeTable.currentBufferSize shouldBe 4
        timeTable.purgeEntriesBefore(4)
        timeTable.entryCount shouldBe 1
        timeTable.currentSum shouldBe 16
    }

    "should be able to purge after buffer was expanded" {
        timeTable.addEntry(32, 4)
        timeTable.addEntry(64, 5)
        timeTable.addEntry(128, 6)
        timeTable.addEntry(256, 7)
        timeTable.entryCount shouldBe 5
        timeTable.currentSum shouldBe 496
        timeTable.currentBufferSize shouldBe 8
        timeTable.purgeEntriesBefore(6)
        timeTable.entryCount shouldBe 2
        timeTable.currentSum shouldBe 384
        timeTable.currentBufferSize shouldBe 8
    }

    "should not be able to shrink buffer below amount of entries" {
        timeTable.addEntry(512, 8)
        timeTable.addEntry(1024, 9)
        timeTable.addEntry(2048, 10)
        timeTable.addEntry(4096, 11)
        shouldThrow<IllegalStateException> {
            timeTable.resizeBuffer(5)
        }.message shouldBe "newBufferSize (5) is to small to hold (6) entries"
    }

    "should return minimum time a sum is reached in overflow position" {
        timeTable.minimumSumReached(3000) shouldBe 10
        timeTable.minimumSumReached(5000) shouldBe 11
        timeTable.minimumSumReached(10000) shouldBe null
    }

    "should be able to purge after buffer was expanded from an overflow position" {
        timeTable.entryCount shouldBe 6
        timeTable.currentSum shouldBe 8064
        timeTable.entryCount shouldBe 6
        timeTable.currentSum shouldBe 8064
        timeTable.currentBufferSize shouldBe 8
        timeTable.purgeEntriesBefore(11)
        timeTable.entryCount shouldBe 1
        timeTable.currentSum shouldBe 4096
        timeTable.currentBufferSize shouldBe 8
    }
})
