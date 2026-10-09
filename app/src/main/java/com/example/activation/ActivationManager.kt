package com.example.activation

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Sealed class representing the current activation/license status.
 */
sealed class LicenseState {
    data object NotActivated : LicenseState()

    data class Active(
        val activationMillis: Long,
        val expiryMillis: Long,
        val years: Int,
        val months: Int,
        val days: Int,
        val remainingMillis: Long
    ) : LicenseState()

    data class Expired(
        val reason: String = "Activation duration has expired."
    ) : LicenseState()
}

/**
 * Data model for a stored activation license.
 */
data class ActivationLicense(
    val activationTimestamp: Long,
    val expiryTimestamp: Long,
    val years: Int,
    val months: Int,
    val days: Int,
    val signature: String
)

/**
 * ActivationManager handles local paid-duration license persistence,
 * secure password verification (SHA-256 hash comparison),
 * calendar-accurate expiry arithmetic, clock-rollback defense,
 * and anti-tamper signature validation.
 */
class ActivationManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _licenseState = MutableStateFlow<LicenseState>(LicenseState.NotActivated)
    val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

    init {
        checkLicenseStatus()
    }

    /**
     * Verifies the entered password against the salted SHA-256 hash.
     * Required password: "12345678"
     */
    fun verifyPassword(input: String): Boolean {
        if (input.isEmpty()) return false
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val saltedBytes = (SALT_PREFIX + input + SALT_SUFFIX).toByteArray(StandardCharsets.UTF_8)
        val computedHashBytes = messageDigest.digest(saltedBytes)
        val computedHashHex = computedHashBytes.joinToString("") { "%02x".format(it) }

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
            computedHashHex.toByteArray(StandardCharsets.UTF_8),
            EXPECTED_HASH.toByteArray(StandardCharsets.UTF_8)
        )
    }

    /**
     * Calculates the exact calendar expiry timestamp.
     * Uses Calendar arithmetic to accurately account for leap years,
     * variable days per month, and preserves the exact time-of-day.
     */
    fun calculateExpiry(
        activationMillis: Long,
        years: Int,
        months: Int,
        days: Int
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = activationMillis
            if (years > 0) add(Calendar.YEAR, years)
            if (months > 0) add(Calendar.MONTH, months)
            if (days > 0) add(Calendar.DAY_OF_MONTH, days)
        }
        return calendar.timeInMillis
    }

    /**
     * Activates the app for the specified duration from the current time.
     * Persists the record securely with an anti-tamper signature.
     */
    @Synchronized
    fun activate(
        years: Int,
        months: Int,
        days: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        // Enforce valid duration constraints
        if (years !in 0..10 || months !in 0..12 || days !in 1..31) {
            return false
        }
        if (years == 0 && months == 0 && days <= 0) {
            return false
        }

        val expiryMillis = calculateExpiry(currentTimeMillis, years, months, days)
        val signature = generateSignature(currentTimeMillis, expiryMillis, years, months, days)

        prefs.edit()
            .putLong(KEY_ACTIVATION_TIME, currentTimeMillis)
            .putLong(KEY_EXPIRY_TIME, expiryMillis)
            .putInt(KEY_DURATION_YEARS, years)
            .putInt(KEY_DURATION_MONTHS, months)
            .putInt(KEY_DURATION_DAYS, days)
            .putString(KEY_SIGNATURE, signature)
            .putLong(KEY_LAST_SEEN_TIME, currentTimeMillis)
            .putBoolean(KEY_IS_ACTIVATED, true)
            .apply()

        _licenseState.value = LicenseState.Active(
            activationMillis = currentTimeMillis,
            expiryMillis = expiryMillis,
            years = years,
            months = months,
            days = days,
            remainingMillis = expiryMillis - currentTimeMillis
        )
        return true
    }

    /**
     * Checks the license status against storage, anti-tamper signature,
     * clock-rollback, and the current system timestamp.
     */
    @Synchronized
    fun checkLicenseStatus(currentTimeMillis: Long = System.currentTimeMillis()): LicenseState {
        val isActivated = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        if (!isActivated) {
            _licenseState.value = LicenseState.NotActivated
            return LicenseState.NotActivated
        }

        val activationTime = prefs.getLong(KEY_ACTIVATION_TIME, 0L)
        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0L)
        val years = prefs.getInt(KEY_DURATION_YEARS, 0)
        val months = prefs.getInt(KEY_DURATION_MONTHS, 0)
        val days = prefs.getInt(KEY_DURATION_DAYS, 0)
        val storedSignature = prefs.getString(KEY_SIGNATURE, "") ?: ""
        val lastSeenTime = prefs.getLong(KEY_LAST_SEEN_TIME, 0L)

        // 1. Anti-Tamper Check: Validate signature
        val expectedSignature = generateSignature(activationTime, expiryTime, years, months, days)
        if (storedSignature.isEmpty() || !MessageDigest.isEqual(
                storedSignature.toByteArray(StandardCharsets.UTF_8),
                expectedSignature.toByteArray(StandardCharsets.UTF_8)
            )
        ) {
            _licenseState.value = LicenseState.Expired("Integrity verification failed.")
            return _licenseState.value
        }

        // 2. Anti-Clock-Rollback: Detect if device time jumped backward significantly
        if (lastSeenTime > 0L && currentTimeMillis < lastSeenTime - CLOCK_ROLLBACK_TOLERANCE_MS) {
            _licenseState.value = LicenseState.Expired("Device clock manipulation detected.")
            return _licenseState.value
        }

        // Update last seen timestamp
        prefs.edit().putLong(KEY_LAST_SEEN_TIME, maxOf(lastSeenTime, currentTimeMillis)).apply()

        // 3. Expiry Check
        if (currentTimeMillis >= expiryTime) {
            _licenseState.value = LicenseState.Expired("Access duration expired.")
            return _licenseState.value
        }

        // 4. Valid Active State
        val activeState = LicenseState.Active(
            activationMillis = activationTime,
            expiryMillis = expiryTime,
            years = years,
            months = months,
            days = days,
            remainingMillis = expiryTime - currentTimeMillis
        )
        _licenseState.value = activeState
        return activeState
    }

    /**
     * Returns true if there is an active, unexpired license.
     */
    fun isLicenseActive(): Boolean {
        return checkLicenseStatus() is LicenseState.Active
    }

    /**
     * Clears or locks the license when expired or revoked.
     */
    fun lockLicense() {
        prefs.edit().putBoolean(KEY_IS_ACTIVATED, false).apply()
        _licenseState.value = LicenseState.Expired("Access locked.")
    }

    private fun generateSignature(
        actTime: Long,
        expTime: Long,
        y: Int,
        m: Int,
        d: Int
    ): String {
        val payload = "$actTime|$expTime|$y|$m|$d|$SIGNATURE_SECRET"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(payload.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    companion object {
        private const val PREFS_NAME = "ludo_controller_activation_vault"
        private const val KEY_IS_ACTIVATED = "k_is_act"
        private const val KEY_ACTIVATION_TIME = "k_act_time"
        private const val KEY_EXPIRY_TIME = "k_exp_time"
        private const val KEY_DURATION_YEARS = "k_dur_y"
        private const val KEY_DURATION_MONTHS = "k_dur_m"
        private const val KEY_DURATION_DAYS = "k_dur_d"
        private const val KEY_SIGNATURE = "k_sig"
        private const val KEY_LAST_SEEN_TIME = "k_last_seen"

        // Tolerance of 2 minutes for minor device NTP adjustments
        private const val CLOCK_ROLLBACK_TOLERANCE_MS = 120_000L

        // Cryptographic constants (Salt and HMAC secret)
        private const val SALT_PREFIX = "LUDO_NEON_CONTROLLER_SALT_"
        private const val SALT_SUFFIX = "_2026_SECURE_AUTH"
        private const val SIGNATURE_SECRET = "CTRL_VAULT_INTEGRITY_SECRET_987413"

        // SHA-256 of (SALT_PREFIX + "12345678" + SALT_SUFFIX)
        // Verified: "LUDO_NEON_CONTROLLER_SALT_12345678_2026_SECURE_AUTH"
        private val EXPECTED_HASH: String by lazy {
            val raw = SALT_PREFIX + "12345678" + SALT_SUFFIX
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(raw.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }

        fun formatExpiryDate(millis: Long): String {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(millis)
        }

        fun formatDurationSummary(years: Int, months: Int, days: Int): String {
            val parts = mutableListOf<String>()
            if (years > 0) parts.add("$years ${if (years == 1) "Year" else "Years"}")
            if (months > 0) parts.add("$months ${if (months == 1) "Month" else "Months"}")
            if (days > 0) parts.add("$days ${if (days == 1) "Day" else "Days"}")
            return if (parts.isEmpty()) "No duration selected" else "Active for " + parts.joinToString(" ")
        }
    }
}
