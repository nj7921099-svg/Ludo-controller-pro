package com.example.controller.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controller App user preferences and settings.
 * Persisted locally via SharedPreferences ("ludo_controller_prefs").
 */
data class ControllerSettings(
    val hapticEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = false,
    val animationsEnabled: Boolean = true,
    val rememberLastMode: Boolean = true,
    val lastMode: String = "SIMPLE" // "SIMPLE" or "PRO"
)

class ControllerSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ControllerSettings> = _settings.asStateFlow()

    private fun loadSettings(): ControllerSettings {
        return ControllerSettings(
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
            soundEffectsEnabled = prefs.getBoolean(KEY_SOUND, false),
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, true),
            rememberLastMode = prefs.getBoolean(KEY_REMEMBER_LAST_MODE, true),
            lastMode = prefs.getString(KEY_LAST_MODE, "SIMPLE") ?: "SIMPLE"
        )
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settings.value = _settings.value.copy(hapticEnabled = enabled)
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settings.value = _settings.value.copy(soundEffectsEnabled = enabled)
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATIONS, enabled).apply()
        _settings.value = _settings.value.copy(animationsEnabled = enabled)
    }

    fun setRememberLastMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_LAST_MODE, enabled).apply()
        _settings.value = _settings.value.copy(rememberLastMode = enabled)
    }

    fun setLastMode(mode: String) {
        prefs.edit().putString(KEY_LAST_MODE, mode).apply()
        _settings.value = _settings.value.copy(lastMode = mode)
    }

    companion object {
        private const val PREFS_NAME = "ludo_controller_prefs"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_SOUND = "sound_effects_enabled"
        private const val KEY_ANIMATIONS = "animations_enabled"
        private const val KEY_REMEMBER_LAST_MODE = "remember_last_mode"
        private const val KEY_LAST_MODE = "controller_mode"
    }
}
