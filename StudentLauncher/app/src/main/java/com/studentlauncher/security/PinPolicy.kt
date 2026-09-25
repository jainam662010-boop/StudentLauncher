package com.studentlauncher.security

/** Pure rules for the optional PIN. No Android types, so they are unit tested on the JVM. */
object PinPolicy {
    const val MIN_LEN = 4
    const val MAX_LEN = 8
    const val FREE_ATTEMPTS = 5
    const val MAX_LOCKOUT_SEC = 300
    /** "Forgot PIN" cooling-off: long enough that it cannot be done on impulse. */
    const val RESET_DELAY_MS = 24L * 60L * 60L * 1000L

    fun isValid(pin: String): Boolean = pin.length in MIN_LEN..MAX_LEN && pin.all { it in '0'..'9' }

    /** Seconds to wait after [failures] wrong tries in a row: none for four, then 30 s doubling up to 5 min. */
    fun lockoutSeconds(failures: Int): Int {
        if (failures < FREE_ATTEMPTS) return 0
        val step = (failures - FREE_ATTEMPTS).coerceAtMost(4)
        return (30 shl step).coerceAtMost(MAX_LOCKOUT_SEC)
    }

    fun resetRemainingMs(startedAt: Long, now: Long): Long =
        (startedAt + RESET_DELAY_MS - now).coerceAtLeast(0L)
}
