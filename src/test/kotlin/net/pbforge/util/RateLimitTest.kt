package net.pbforge.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

object MockTime {
    var time: Long = 0
}

class RateLimitTest : StringSpec({
    val rateLimit = RateLimit(100, timer = { MockTime.time })

    "should allow acquiring permits" {
        rateLimit.acquire(10) shouldBe 0L
        MockTime.time += 100
        rateLimit.acquire(80) shouldBe 0L
    }

    "should return appropriate wait-times" {
        MockTime.time += 100
        rateLimit.acquire(20) shouldBe 801L
        MockTime.time += 800
        rateLimit.acquire(20) shouldBe 1L
        MockTime.time += 1
        rateLimit.acquire(20) shouldBe 0L
    }

    "should return correct current rate" {
        rateLimit.currentRate shouldBe 100
        MockTime.time += 100
        rateLimit.currentRate shouldBe 20
        MockTime.time += 1899
        rateLimit.currentRate shouldBe 0
    }
    // MockTime.time = 3000
    "should reserve amounts" {
        rateLimit.acquire(40, true) shouldBe 0
        rateLimit.currentRate shouldBe 40
        MockTime.time += 100
        rateLimit.acquire(50, true) shouldBe 0
        rateLimit.currentRate shouldBe 90
        MockTime.time += 100
        rateLimit.acquire(50, true) shouldBe 801
        rateLimit.currentRate shouldBe 90
        MockTime.time += 800
        rateLimit.currentRate shouldBe 90
        MockTime.time += 1
        rateLimit.currentRate shouldBe 100
        MockTime.time += 100
        rateLimit.currentRate shouldBe 50
        MockTime.time += 901
        rateLimit.currentRate shouldBe 0
    }

    "should support multiple reservations" {
        MockTime.time = 0L
        rateLimit.clear()
        rateLimit.acquire(100, true) shouldBe 0
        MockTime.time += 100
        rateLimit.acquire(100, true) shouldBe 901
        MockTime.time += 100
        rateLimit.acquire(100, true) shouldBe 1802
    }
})
