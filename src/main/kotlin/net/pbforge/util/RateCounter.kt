package net.pbforge.util

import java.time.Duration

class RateCounter(
    period: Duration = Duration.ofSeconds(1L),
    private val timeTable: RingBufferTimeTable = RingBufferTimeTable(),
    private val timer: () -> Long = System::currentTimeMillis
) {
    private var first: Long? = null
    private val periodMillis = period.toMillis()

    fun info() = timeTable.info()

    fun count(): Long? {
        val now = timer()
        val oneSecondBefore = now - periodMillis
        timeTable.purgeEntriesBefore(oneSecondBefore)

        timeTable.addEntry(1L, now)
        val f = first
        if (f == null) {
            first = now
            return null
        } else if (now - f < periodMillis) {
            return null
        }
        return timeTable.currentSum
    }
}
