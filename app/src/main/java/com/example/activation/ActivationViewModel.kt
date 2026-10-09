package com.example.activation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Navigation steps inside the activation flow when license is not active.
 */
enum class ActivationScreenStep {
    PASSWORD_REQUIRED,
    DURATION_SELECTION
}

class ActivationViewModel(application: Application) : AndroidViewModel(application) {

    val activationManager = ActivationManager(application)
    val licenseState: StateFlow<LicenseState> = activationManager.licenseState

    private val _screenStep = MutableStateFlow(ActivationScreenStep.PASSWORD_REQUIRED)
    val screenStep: StateFlow<ActivationScreenStep> = _screenStep.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _passwordVisible = MutableStateFlow(false)
    val passwordVisible: StateFlow<Boolean> = _passwordVisible.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    // Default duration: 1 Year, 0 Months, 1 Day (or user can customize)
    private val _selectedYears = MutableStateFlow(1)
    val selectedYears: StateFlow<Int> = _selectedYears.asStateFlow()

    private val _selectedMonths = MutableStateFlow(0)
    val selectedMonths: StateFlow<Int> = _selectedMonths.asStateFlow()

    private val _selectedDays = MutableStateFlow(1)
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()

    private val _summaryText = MutableStateFlow("")
    val summaryText: StateFlow<String> = _summaryText.asStateFlow()

    private val _previewExpiryFormatted = MutableStateFlow("")
    val previewExpiryFormatted: StateFlow<String> = _previewExpiryFormatted.asStateFlow()

    private val _isActivating = MutableStateFlow(false)
    val isActivating: StateFlow<Boolean> = _isActivating.asStateFlow()

    init {
        updatePreviews()
        startPeriodicExpiryWatcher()
    }

    /**
     * Watches for real-time expiry even while the app remains open in the foreground.
     * Only transitions back to password if the license was previously ACTIVE and expired.
     * While the user has unlocked the password and is on the DURATION_SELECTION screen,
     * it will not kick them back to PASSWORD_REQUIRED.
     */
    private fun startPeriodicExpiryWatcher() {
        viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                val current = activationManager.checkLicenseStatus()
                // If a previously active license expires while the user is using the app
                if (current is LicenseState.Expired) {
                    if (_screenStep.value != ActivationScreenStep.PASSWORD_REQUIRED) {
                        _screenStep.value = ActivationScreenStep.PASSWORD_REQUIRED
                    }
                }
            }
        }
    }

    fun onPasswordChanged(value: String) {
        _passwordInput.value = value
        if (_passwordError.value != null) {
            _passwordError.value = null
        }
    }

    fun togglePasswordVisibility() {
        _passwordVisible.value = !_passwordVisible.value
    }

    fun submitPassword(): Boolean {
        val trimmed = _passwordInput.value.trim()
        val isValid = activationManager.verifyPassword(trimmed)
        if (isValid) {
            _passwordError.value = null
            _passwordInput.value = ""
            _screenStep.value = ActivationScreenStep.DURATION_SELECTION
            updatePreviews()
            return true
        } else {
            _passwordError.value = "Incorrect access key. Please enter the valid password."
            return false
        }
    }

    fun onYearChanged(year: Int) {
        _selectedYears.value = year.coerceIn(0, 10)
        updatePreviews()
    }

    fun onMonthChanged(month: Int) {
        _selectedMonths.value = month.coerceIn(0, 12)
        updatePreviews()
    }

    fun onDayChanged(day: Int) {
        _selectedDays.value = day.coerceIn(1, 31)
        updatePreviews()
    }

    private fun updatePreviews() {
        val y = _selectedYears.value
        val m = _selectedMonths.value
        val d = _selectedDays.value
        _summaryText.value = ActivationManager.formatDurationSummary(y, m, d)

        val now = System.currentTimeMillis()
        val expiryMillis = activationManager.calculateExpiry(now, y, m, d)
        _previewExpiryFormatted.value = ActivationManager.formatExpiryDate(expiryMillis)
    }

    fun activate(): Boolean {
        val y = _selectedYears.value
        val m = _selectedMonths.value
        val d = _selectedDays.value

        _isActivating.value = true
        val success = activationManager.activate(y, m, d)
        _isActivating.value = false

        if (success) {
            // Once activated, the gate will automatically switch to the Controller Home
            _passwordInput.value = ""
            _passwordError.value = null
        }
        return success
    }

    fun backToPassword() {
        _screenStep.value = ActivationScreenStep.PASSWORD_REQUIRED
        _passwordInput.value = ""
        _passwordError.value = null
    }

    fun checkStatusOnResume() {
        val current = activationManager.checkLicenseStatus()
        // If expired, always force back to password requirement
        if (current is LicenseState.Expired) {
            _screenStep.value = ActivationScreenStep.PASSWORD_REQUIRED
        }
    }
}
