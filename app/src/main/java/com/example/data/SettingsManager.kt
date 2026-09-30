package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FeatureStatus(val label: String) {
    ENABLED("Enabled"),
    DISABLED("Disabled"),
    UNSUPPORTED("Permission Needed")
}

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("breadcrumb_settings", Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _floatingOverlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_FLOATING_OVERLAY, true))
    val floatingOverlayEnabled: StateFlow<Boolean> = _floatingOverlayEnabled.asStateFlow()

    private val _textSelectionEnabled = MutableStateFlow(prefs.getBoolean(KEY_TEXT_SELECTION, true))
    val textSelectionEnabled: StateFlow<Boolean> = _textSelectionEnabled.asStateFlow()

    init {
        syncProcessTextAliasState(_textSelectionEnabled.value)
    }

    fun setPro(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, enabled).apply()
        _isPro.value = enabled
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FLOATING_OVERLAY, enabled).apply()
        _floatingOverlayEnabled.value = enabled
    }

    fun setTextSelection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TEXT_SELECTION, enabled).apply()
        _textSelectionEnabled.value = enabled
        syncProcessTextAliasState(enabled)
    }

    private fun syncProcessTextAliasState(enabled: Boolean) {
        try {
            val componentName = ComponentName(context, "com.example.ProcessTextAlias")
            val newState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_IS_PRO = "key_is_pro"
        private const val KEY_FLOATING_OVERLAY = "key_floating_overlay"
        private const val KEY_TEXT_SELECTION = "key_text_selection"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
