package net.pbforge.util

import java.time.Duration

class RateLimit(
        private val amount: Long,
        period: Duration = Duration.ofSeconds(1L),
        private val timeTable: RingBufferTimeTable = RingBufferTimeTable(),
        private val timer: () -> Long = System::currentTimeMillis
) {
    private val period = period.toMillis()
    val currentRate: Long
        get() {
            val now = timer()
            timeTable.purgeEntriesBefore(now - period)
            return timeTable.sumBefore(now + 1)
        }

    fun clear() {
        timeTable.purgeEntriesBefore(Long.MAX_VALUE)
    }

    fun acquire(
            requestedAmount: Long = 1L,
            reserve: Boolean = false,
            absoluteTime: Boolean = false,
    ): Long {
        val now = timer()
        timeTable.purgeEntriesBefore(now - period)
        val amountMissing = timeTable.currentSum + requestedAmount - amount
        if (amountMissing <= 0) {
            timeTable.addEntry(requestedAmount, now)
            return 0L
        }
        val waitUntilWindowMovesPast = timeTable.minimumSumReached(amountMissing)
                ?: throw IllegalArgumentException("Requested amount (${this.amount}) surpasses maximum-amount ($amount)")
        val reservationEffectiveAtTime = waitUntilWindowMovesPast + period + 1
        if (reserve) {
            timeTable.addEntry(requestedAmount, reservationEffectiveAtTime)
        }
        if (absoluteTime) {
            return reservationEffectiveAtTime
        }
        return reservationEffectiveAtTime - now
    }
}
