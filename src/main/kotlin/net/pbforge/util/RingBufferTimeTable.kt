package net.pbforge.util

/**
 * A ring-buffer implementation of a TimeTable.
 *
 * This class is *not* multi-thread safe.
 *
 * @param bufferSize to use for this table
 * @param maxBufferSize to which the table can grow
 */
class RingBufferTimeTable(
        private var bufferSize: Int = 64,
        val maxBufferSize: Int = bufferSize * 64,
) : TimeTable {
    private var values = LongArray(bufferSize)
    private var times = LongArray(bufferSize)
    private var writePtr = 0
    private var readPtr = 0
    private var sum = 0L

    init {
        check(bufferSize >= 2) { "bufferSize must be 2 or bigger" }
        check(maxBufferSize >= bufferSize) { "maxBufferSize must be at least bufferSize" }
    }

    /**
     * The current size of the internal buffer.
     */
    val currentBufferSize: Int get() = bufferSize

    @OptIn(ExperimentalStdlibApi::class)
    override fun entries(): List<TimeTable.TimeTableEntry> = buildList {
        processEntriesForward { amount, time ->
            this.add(TimeTable.TimeTableEntry(time, amount))
        }
    }

    override val currentSum: Long get() = sum

    override val entryCount: Int
        get() = if (readPtr <= writePtr) {
            writePtr - readPtr
        } else {
            bufferSize - readPtr + writePtr
        }

    override val lastTime: Long?
        get() = if (readPtr == writePtr) {
            null
        } else {
            times[(writePtr - 1 + bufferSize) % bufferSize]
        }

    override val firstTime: Long?
        get() = if (readPtr == writePtr) {
            null
        } else {
            times[readPtr]
        }

    override fun purgeEntriesBefore(deadline: Long) {
        while (readPtr != writePtr && times[readPtr] < deadline) {
            sum -= values[readPtr]
            readPtr = (readPtr + 1) % bufferSize
        }
    }

    override fun clear() {
        readPtr = 0
        writePtr = 0
        sum = 0
    }

    override fun addEntry(value: Long, time: Long) {
        check(time >= 0) { "Negative time" }
        check(readPtr == writePtr || times[(writePtr - 1 + bufferSize) % bufferSize] <= time) {
            "An entry that happened after the specified time already exist (${
                times[(writePtr - 1 + bufferSize) % bufferSize] - time} time-units before)"
        }
        check(value < Long.MAX_VALUE - sum) { "Addition causes sum to overflow" }
        var nextWritePtr = (writePtr + 1) % bufferSize
        if (nextWritePtr == readPtr) {
            resizeBuffer(bufferSize * 2)
            nextWritePtr = writePtr + 1
        }
        values[writePtr] = value
        times[writePtr] = time
        writePtr = nextWritePtr
        sum += value
    }

    /**
     * Resize the current buffer to the supplied value. This method can be used to expand or shrink the buffer.
     *
     * @param newBufferSize to use
     */
    fun resizeBuffer(newBufferSize: Int) {
        check(newBufferSize <= maxBufferSize) {
            "newBufferSize ($newBufferSize) is bigger than the maxBufferSize ($maxBufferSize)"
        }
        val entries = entryCount
        check(newBufferSize > entryCount) {
            "newBufferSize ($newBufferSize) is to small to hold ($entries) entries"
        }
        val newTimes = LongArray(newBufferSize)
        val newValues = LongArray(newBufferSize)
        var newWritePtr = 0
        while (readPtr != writePtr) {
            newTimes[newWritePtr] = times[readPtr]
            newValues[newWritePtr++] = values[readPtr]
            readPtr = (readPtr + 1) % bufferSize
        }
        times = newTimes
        values = newValues
        writePtr = newWritePtr
        readPtr = 0
        bufferSize = newBufferSize
    }

    override fun minimumSumReached(requiredSum: Long): Long? {
        var readAheadSum = 0L
        processEntriesForward { value, time ->
            readAheadSum += value
            if (readAheadSum >= requiredSum) {
                return time
            }
        }
        return null
    }

    override fun sumAfter(deadline: Long): Long {
        var sum = 0L
        processEntriesBackwards { value, time ->
            if (time <= deadline) {
                return sum
            }
            sum += value
        }
        return sum
    }

    override fun sumBefore(deadline: Long): Long {
        var sum = 0L
        processEntriesForward { value, time ->
            if (time >= deadline) {
                return sum
            }
            sum += value
        }
        return sum
    }

    private inline fun processEntriesForward(processor: (value: Long, time: Long) -> Unit) {
        var readAheadPtr = readPtr
        while (readAheadPtr != writePtr) {
            processor(values[readAheadPtr], times[readAheadPtr])
            readAheadPtr = (readAheadPtr + 1) % bufferSize
        }
    }

    private inline fun processEntriesBackwards(processor: (value: Long, time: Long) -> Unit) {
        if (readPtr == writePtr) {
            return
        }
        var readAheadPtr = writePtr
        do {
            readAheadPtr = (readAheadPtr - 1 + bufferSize) % bufferSize
            processor(values[readAheadPtr], times[readAheadPtr])
        } while (readAheadPtr != readPtr)
    }
}
