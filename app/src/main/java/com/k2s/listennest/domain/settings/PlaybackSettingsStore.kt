package com.k2s.listennest.domain.settings

import android.content.Context
import android.content.SharedPreferences

private const val PLAYBACK_PREFS_NAME = "playback_settings"
private const val KEY_PHONE_CALL_PAUSE_ENABLED = "phone_call_pause_enabled"

class PlaybackSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PLAYBACK_PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun isPhoneCallPauseEnabled(): Boolean =
        prefs.getBoolean(KEY_PHONE_CALL_PAUSE_ENABLED, false)

    fun setPhoneCallPauseEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_PHONE_CALL_PAUSE_ENABLED, enabled)
            .apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
