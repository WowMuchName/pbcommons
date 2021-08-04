package net.pbforge.util

import java.time.Duration

class TickLimit(
    private val amount: Long,
    period: Duration = Duration.ofSeconds(1L),
    private val timeTable: RingBufferTimeTable = RingBufferTimeTable(),
    private val timer: () -> Long = System::currentTimeMillis
) {
    private val period = period.toMillis()

    fun info() = timeTable.info()

    fun acquire(): Long {
        var now = timer()
        val last = timeTable.lastTime
        if (last == null) {
            timeTable.addEntry(1L, now)
            return 0
        }
        val first = timeTable.firstTime!!
        val timeElapsedThisTick = now - last
        val timeLeftInPeriodBeforeTick = period - (last - first)
        val ticksInPeriod = timeTable.entryCount
        val timePerTick = timeLeftInPeriodBeforeTick / (amount + 1 - ticksInPeriod)
        val wait = (timePerTick - timeElapsedThisTick).coerceAtLeast(0)
        if (wait > 0) {
            Thread.sleep(wait)
        }
        now = timer()
        timeTable.addEntry(1L, now)
        if (timeTable.entryCount + 1 == amount.toInt()) {
            timeTable.purgeEntriesBefore(now)
        }
        return now - last
    }
}
