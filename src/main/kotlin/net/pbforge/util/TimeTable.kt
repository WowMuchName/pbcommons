package net.pbforge.util

import kotlin.math.max
import kotlin.math.min

/**
 * The class TimeTable is a collection that holds value-time pairs and keeps a sum of all values.
 */
interface TimeTable {
    data class TimeTableEntry(
        val time: Long,
        val amount: Long,
    )

    data class TimePeriodInfo(
        val maximum: Long,
        val minimum: Long,
        val average: Long,
    )

    fun info(): TimePeriodInfo {
        var maximum = 0L
        var minimum = Long.MAX_VALUE
        var average = 0L
        var last = -1L
        var count = 0
        entries().forEach {
            if (last != -1L) {
                val time = it.time - last
                maximum = max(maximum, time)
                minimum = min(minimum, time)
                average += time
                count++
            }
            last = it.time
        }
        return TimePeriodInfo(maximum, minimum, if(count == 0) { 0 } else { average / count} )
    }

    fun entries(): List<TimeTableEntry>
    /**
     * The sum of the values specified in the entries currently in this table.
     */
    val currentSum: Long

    /**
     * The amount of entries that are currently stored in this table.
     */
    val entryCount: Int

    /**
     * The time of the last entry, null if none exists.
     */
    val lastTime: Long?

    /**
     * Removes all entries that happened before the supplied amount of time. The currentSum is applied accordingly.
     *
     * @param deadline before which entries will be removed
     */
    fun purgeEntriesBefore(deadline: Long)

    /**
     * Removes all entries.
     */
    fun clear()

    /**
     * Add a new entry for the provided amount and the provided time.
     *
     * @param value associated with the new entry
     * @param time associated with the new entry
     * @throws IllegalStateException if the provided time is before an already present time
     */
    fun addEntry(value: Long, time: Long)

    /**
     * The time at which the sum of values reached or surpasses the desired value.
     *
     * @param requiredSum
     * @return earliest time where the requiredSum is reached or null if it never does
     */
    fun minimumSumReached(requiredSum: Long): Long?

    /**
     * The sum of values after a supplied <code>deadline</code>.
     *
     * @param deadline for entries
     * @return the sum after the deadline
     */
    fun sumAfter(deadline: Long): Long

    /**
     * The sum of values before a supplied <code>deadline</code>.
     *
     * @param deadline for entries
     * @return the sum before the deadline
     */
    fun sumBefore(deadline: Long): Long
    val firstTime: Long?
}
