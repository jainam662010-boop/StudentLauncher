package com.studentlauncher.security

import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Optional PIN that protects the distraction list (and, if you choose, Study mode).
 * Only a salted PBKDF2 hash is stored, never the PIN. Wrong tries lock the pad for a growing time.
 * This is a speed bump against impulse, not military security: it lives in the launcher's private prefs.
 */
class PinLock(
    private val prefs: SharedPreferences,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    val isSet: Boolean get() = prefs.getString(K_HASH, null) != null

    var protectStudyMode: Boolean
        get() = prefs.getBoolean(K_STUDY, false)
        set(value) { prefs.edit().putBoolean(K_STUDY, value).apply() }

    fun set(pin: String): Boolean {
        if (!PinPolicy.isValid(pin)) return false
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(K_SALT, encode(salt))
            .putString(K_HASH, encode(hash(pin, salt)))
            .remove(K_FAILS).remove(K_LOCK).remove(K_RESET)
            .apply()
        return true
    }

    fun clear() {
        prefs.edit().remove(K_HASH).remove(K_SALT).remove(K_FAILS).remove(K_LOCK).remove(K_RESET).remove(K_STUDY).apply()
    }

    fun lockedForSec(): Int {
        val until = prefs.getLong(K_LOCK, 0L)
        return ((until - clock() + 999L) / 1000L).toInt().coerceAtLeast(0)
    }

    /** True when the PIN is right. Wrong tries count towards the lockout; a locked pad always answers false. */
    fun verify(pin: String): Boolean {
        if (!isSet || lockedForSec() > 0) return false
        val salt = prefs.getString(K_SALT, null)?.let { decode(it) } ?: return false
        val expected = prefs.getString(K_HASH, null)?.let { decode(it) } ?: return false
        val ok = MessageDigest.isEqual(hash(pin, salt), expected)
        val e = prefs.edit()
        if (ok) {
            e.remove(K_FAILS).remove(K_LOCK)
        } else {
            val fails = prefs.getInt(K_FAILS, 0) + 1
            e.putInt(K_FAILS, fails)
            val wait = PinPolicy.lockoutSeconds(fails)
            if (wait > 0) e.putLong(K_LOCK, clock() + wait * 1000L)
        }
        e.apply()
        return ok
    }

    // ---- forgot PIN: a 24 hour cooling-off, then the PIN can be removed ----
    fun startReset() {
        if (prefs.getLong(K_RESET, 0L) == 0L) prefs.edit().putLong(K_RESET, clock()).apply()
    }

    fun cancelReset() { prefs.edit().remove(K_RESET).apply() }

    /** Milliseconds until the PIN can be removed, or null when no reset was started. */
    fun resetRemainingMs(): Long? {
        val started = prefs.getLong(K_RESET, 0L)
        return if (started == 0L) null else PinPolicy.resetRemainingMs(started, clock())
    }

    /** Removes the PIN once the cooling-off is over. Returns true when it did. */
    fun finishResetIfDue(): Boolean {
        val left = resetRemainingMs() ?: return false
        if (left > 0L) return false
        clear()
        return true
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun encode(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun decode(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    private companion object {
        const val K_HASH = "pin_hash"
        const val K_SALT = "pin_salt"
        const val K_FAILS = "pin_fails"
        const val K_LOCK = "pin_lock_until"
        const val K_RESET = "pin_reset_started"
        const val K_STUDY = "pin_protect_study"
        const val SALT_BYTES = 16
        const val ITERATIONS = 20_000
        const val KEY_BITS = 256
    }
}
