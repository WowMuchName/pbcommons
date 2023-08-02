package net.pbforge.crypto

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

val DEFAULT_VALIDITY_TIMESPAN = ValidityTimespan()

class ValidityTimespan(
    val notBefore: Instant = Instant.now(),
    val notAfter: Instant = Instant.now().plus(365, ChronoUnit.DAYS),
    val timezoneLocale: Locale = Locale.getDefault(),
)
