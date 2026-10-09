package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.activation.ActivationManager
import com.example.activation.LicenseState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivationUnitTest {

    private lateinit var manager: ActivationManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Clear shared preferences for clean test run
        context.getSharedPreferences("ludo_controller_activation_vault", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        manager = ActivationManager(context)
    }

    @Test
    fun testInitialLaunchIsNotActivated() {
        assertFalse("Fresh launch must not be activated", manager.isLicenseActive())
        val state = manager.checkLicenseStatus()
        assertTrue("State must be NotActivated", state is LicenseState.NotActivated)
    }

    @Test
    fun testPasswordVerification() {
        assertFalse("Empty password must fail", manager.verifyPassword(""))
        assertFalse("Wrong password must fail", manager.verifyPassword("password"))
        assertFalse("Wrong digits must fail", manager.verifyPassword("1234"))
        assertFalse("Wrong 8-digits must fail", manager.verifyPassword("87654321"))
        assertFalse("Wrong digits must fail", manager.verifyPassword("00000000"))

        // Exact password required by specifications: 12345678
        assertTrue("Correct password '12345678' must succeed", manager.verifyPassword("12345678"))
    }

    @Test
    fun testCalendarArithmeticExpiryCalculation() {
        // Sample activation: 9 October 2026, 15:30:45
        val activationCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 9)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 45)
            set(Calendar.MILLISECOND, 0)
        }
        val activationMillis = activationCal.timeInMillis

        // Add 1 Year, 2 Months, 10 Days
        val expiryMillis = manager.calculateExpiry(
            activationMillis = activationMillis,
            years = 1,
            months = 2,
            days = 10
        )

        val expiryCal = Calendar.getInstance().apply {
            timeInMillis = expiryMillis
        }

        // Year: 2026 + 1 = 2027
        assertEquals("Year must be 2027", 2027, expiryCal.get(Calendar.YEAR))
        // Month: October (9) + 2 = December (11)
        assertEquals("Month must be December", Calendar.DECEMBER, expiryCal.get(Calendar.MONTH))
        // Day: 9 + 10 = 19
        assertEquals("Day must be 19", 19, expiryCal.get(Calendar.DAY_OF_MONTH))
        // Time of day must be preserved: 15:30:45
        assertEquals("Hour must be preserved", 15, expiryCal.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute must be preserved", 30, expiryCal.get(Calendar.MINUTE))
        assertEquals("Second must be preserved", 45, expiryCal.get(Calendar.SECOND))
    }

    @Test
    fun testActivationAndPersistenceAcrossRestarts() {
        val now = System.currentTimeMillis()
        val success = manager.activate(years = 1, months = 3, days = 15, currentTimeMillis = now)
        assertTrue("Activation must succeed", success)
        assertTrue("License must now be active", manager.isLicenseActive())

        val state = manager.checkLicenseStatus(now)
        assertTrue("State must be Active", state is LicenseState.Active)
        val active = state as LicenseState.Active
        assertEquals(1, active.years)
        assertEquals(3, active.months)
        assertEquals(15, active.days)

        // Simulate app restart / Activity recreation / new manager instance
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val newManager = ActivationManager(context)
        assertTrue("New manager after restart must remain active", newManager.isLicenseActive())
        val restoredState = newManager.checkLicenseStatus(now)
        assertTrue("Restored state must be Active", restoredState is LicenseState.Active)
    }

    @Test
    fun testExpiryDetection() {
        val now = 1770000000000L // arbitrary base time
        manager.activate(years = 0, months = 0, days = 5, currentTimeMillis = now)
        val expiry = manager.calculateExpiry(now, 0, 0, 5)

        // Before expiry: Active
        val beforeExpiry = manager.checkLicenseStatus(expiry - 1000L)
        assertTrue("Before expiry must be Active", beforeExpiry is LicenseState.Active)

        // Exactly at or after expiry: Expired
        val atExpiry = manager.checkLicenseStatus(expiry)
        assertTrue("At expiry must be Expired", atExpiry is LicenseState.Expired)
        assertFalse("isLicenseActive must return false when expired", manager.isLicenseActive())

        val afterExpiry = manager.checkLicenseStatus(expiry + 100000L)
        assertTrue("After expiry must be Expired", afterExpiry is LicenseState.Expired)
    }

    @Test
    fun testReactivationAfterExpiry() {
        val now = 1770000000000L
        manager.activate(years = 0, months = 0, days = 1, currentTimeMillis = now)
        val expiry = manager.calculateExpiry(now, 0, 0, 1)

        // Force expired status
        val expiredState = manager.checkLicenseStatus(expiry + 1000L)
        assertTrue("Must be expired", expiredState is LicenseState.Expired)

        // User enters password
        assertTrue(manager.verifyPassword("12345678"))

        // User selects new duration: 2 Years, 0 Months, 10 Days
        val reactivateTime = expiry + 5000L
        val reactivated = manager.activate(years = 2, months = 0, days = 10, currentTimeMillis = reactivateTime)
        assertTrue("Reactivation must succeed", reactivated)

        val newStatus = manager.checkLicenseStatus(reactivateTime)
        assertTrue("Must be active again", newStatus is LicenseState.Active)
        val active = newStatus as LicenseState.Active
        assertEquals(2, active.years)
        assertEquals(10, active.days)
    }

    @Test
    fun testTamperAndClockRollbackDetection() {
        val now = 1770000000000L
        manager.activate(years = 1, months = 0, days = 1, currentTimeMillis = now)
        assertTrue(manager.isLicenseActive())

        // Simulate clock rollback of 10 hours
        val rolledBackTime = now - (10 * 3600 * 1000L)
        val status = manager.checkLicenseStatus(rolledBackTime)
        assertTrue("Clock rollback must be flagged as Expired", status is LicenseState.Expired)
    }

    @Test
    fun testPasswordUnlockDoesNotGetImmediatelyReverted() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.activation.ActivationViewModel(app)

        assertEquals(
            com.example.activation.ActivationScreenStep.PASSWORD_REQUIRED,
            vm.screenStep.value
        )

        // Type correct password and submit
        vm.onPasswordChanged("12345678")
        val success = vm.submitPassword()
        assertTrue("Password submission must succeed", success)

        // Must transition to DURATION_SELECTION and remain there
        assertEquals(
            com.example.activation.ActivationScreenStep.DURATION_SELECTION,
            vm.screenStep.value
        )

        // Simulate onResume
        vm.checkStatusOnResume()
        assertEquals(
            "Screen step must stay on DURATION_SELECTION",
            com.example.activation.ActivationScreenStep.DURATION_SELECTION,
            vm.screenStep.value
        )
    }
}
